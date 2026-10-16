package com.duolinfo.ia.proj.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Base64;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProfilePictureStorageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(
        ProfilePictureStorageService.class);
    private static final String PUBLIC_PATH = "/media/profile-pictures/";
    private static final int MAX_IMAGE_BYTES = 5 * 1024 * 1024;
    private static final int MAX_DATA_URL_LENGTH = 7_000_000;
    private static final int MAX_DIMENSION = 1024;
    private static final HttpClient GOOGLE_IMAGE_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(3))
        .followRedirects(HttpClient.Redirect.NEVER)
        .build();
    private static final Pattern STORED_FILE_NAME = Pattern.compile(
        "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png)$");
    private static final Pattern STORED_PATH = Pattern.compile(
        "^/media/profile-pictures/([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(?:jpg|png))$");

    private final Path storageDirectory;

    public ProfilePictureStorageService(
            @Value("${app.storage.profile-pictures:./uploads/profile-pictures}")
            String storageDirectory) {
        this.storageDirectory = Path.of(storageDirectory)
            .toAbsolutePath()
            .normalize();
    }

    public String store(String picture) {
        if (picture == null || picture.isBlank()) {
            return null;
        }

        String value = picture.trim();
        if (isStoredPicturePath(value)) {
            return value;
        }
        if (value.regionMatches(true, 0, "data:", 0, 5)) {
            return storeDataUrl(value);
        }

        return validateExternalUrl(value);
    }

    public String importGooglePicture(String pictureUrl) {
        if (pictureUrl == null || pictureUrl.isBlank()) {
            return null;
        }

        try {
            return downloadGooglePicture(pictureUrl.trim());
        } catch (IOException | InterruptedException | IllegalArgumentException
                | ResponseStatusException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            LOGGER.warn("Não foi possível importar a foto de perfil do Google.", exception);
            return null;
        }
    }

    public boolean isGooglePictureUrl(String pictureUrl) {
        if (pictureUrl == null || pictureUrl.isBlank()) {
            return false;
        }

        try {
            validateGooglePictureUri(pictureUrl.trim());
            return true;
        } catch (IOException | IllegalArgumentException exception) {
            return false;
        }
    }

    public boolean isInlinePicture(String picture) {
        return picture != null
            && picture.regionMatches(true, 0, "data:", 0, 5);
    }

    public boolean isStoredPicturePath(String picture) {
        return picture != null && STORED_PATH.matcher(picture).matches();
    }

    public void delete(String picture) {
        String fileName = storedFileName(picture);
        if (fileName == null) {
            return;
        }

        try {
            Files.deleteIfExists(storageDirectory.resolve(fileName));
        } catch (IOException exception) {
            LOGGER.warn("Não foi possível remover uma foto de perfil substituída.", exception);
        }
    }

    public StoredPicture load(String fileName) {
        if (fileName == null || !STORED_FILE_NAME.matcher(fileName).matches()) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Foto de perfil não encontrada.");
        }

        Path picturePath = storageDirectory.resolve(fileName).normalize();
        if (!picturePath.startsWith(storageDirectory) || !Files.isRegularFile(picturePath)) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Foto de perfil não encontrada.");
        }

        String contentType = fileName.endsWith(".png")
            ? "image/png"
            : "image/jpeg";
        return new StoredPicture(new FileSystemResource(picturePath), contentType);
    }

    private String storeDataUrl(String dataUrl) {
        if (dataUrl.length() > MAX_DATA_URL_LENGTH) {
            throw badRequest("A foto deve ter no máximo 5 MB.");
        }

        int separator = dataUrl.indexOf(',');
        if (separator < 0) {
            throw badRequest("O formato da foto de perfil é inválido.");
        }

        String mediaType = dataUrl.substring(0, separator).toLowerCase(Locale.ROOT);
        if (!mediaType.equals("data:image/png;base64")
                && !mediaType.equals("data:image/jpeg;base64")
                && !mediaType.equals("data:image/jpg;base64")) {
            throw badRequest("A foto deve estar no formato JPG ou PNG.");
        }

        byte[] imageBytes;
        try {
            imageBytes = Base64.getDecoder().decode(dataUrl.substring(separator + 1));
        } catch (IllegalArgumentException exception) {
            throw badRequest("O conteúdo da foto de perfil está corrompido.");
        }

        String extension = validateImage(imageBytes, mediaType);
        return writeImage(imageBytes, extension);
    }

    private String downloadGooglePicture(String pictureUrl)
            throws IOException, InterruptedException {
        URI pictureUri = validateGooglePictureUri(pictureUrl);
        HttpRequest request = HttpRequest.newBuilder(pictureUri)
            .timeout(Duration.ofSeconds(8))
            .header("Accept", "image/png,image/jpeg")
            .GET()
            .build();
        HttpResponse<InputStream> response = GOOGLE_IMAGE_CLIENT.send(
            request,
            HttpResponse.BodyHandlers.ofInputStream());

        try (InputStream imageStream = response.body()) {
            if (response.statusCode() != 200) {
                throw new IOException("O Google não retornou a foto de perfil.");
            }

            String contentType = response.headers()
                .firstValue("Content-Type")
                .orElse("")
                .split(";", 2)[0]
                .trim()
                .toLowerCase(Locale.ROOT);
            String declaredMediaType = switch (contentType) {
                case "image/png" -> "data:image/png;base64";
                case "image/jpeg", "image/jpg" -> "data:image/jpeg;base64";
                default -> throw new IOException(
                    "O Google retornou um formato de imagem não suportado.");
            };

            long contentLength = response.headers()
                .firstValueAsLong("Content-Length")
                .orElse(-1);
            if (contentLength > MAX_IMAGE_BYTES) {
                throw new IOException("A foto de perfil do Google excede o tamanho permitido.");
            }

            byte[] imageBytes = imageStream.readNBytes(MAX_IMAGE_BYTES + 1);
            if (imageBytes.length > MAX_IMAGE_BYTES) {
                throw new IOException("A foto de perfil do Google excede o tamanho permitido.");
            }

            String extension = validateImage(imageBytes, declaredMediaType);
            return writeImage(imageBytes, extension);
        }
    }

    private static URI validateGooglePictureUri(String pictureUrl) throws IOException {
        URI uri;
        try {
            uri = URI.create(pictureUrl);
        } catch (IllegalArgumentException exception) {
            throw new IOException("A URL da foto do Google é inválida.", exception);
        }

        String host = uri.getHost();
        boolean trustedGoogleHost = host != null
            && (host.equalsIgnoreCase("googleusercontent.com")
                || host.toLowerCase(Locale.ROOT).endsWith(".googleusercontent.com"));
        if (!"https".equalsIgnoreCase(uri.getScheme())
                || !trustedGoogleHost
                || uri.getUserInfo() != null
                || (uri.getPort() != -1 && uri.getPort() != 443)) {
            throw new IOException("A URL da foto do Google não é confiável.");
        }
        return uri;
    }

    private String validateExternalUrl(String value) {
        if (value.length() > 2048) {
            throw badRequest("A URL da foto de perfil é muito longa.");
        }

        try {
            URI uri = URI.create(value);
            String scheme = uri.getScheme();
            if (scheme == null
                    || (!scheme.equalsIgnoreCase("https")
                        && !scheme.equalsIgnoreCase("http"))
                    || uri.getHost() == null
                    || uri.getUserInfo() != null) {
                throw badRequest("Informe uma URL de imagem HTTP ou HTTPS válida.");
            }
            return uri.toASCIIString();
        } catch (IllegalArgumentException exception) {
            throw badRequest("Informe uma URL de imagem HTTP ou HTTPS válida.");
        }
    }

    private String validateImage(byte[] imageBytes, String declaredMediaType) {
        if (imageBytes.length == 0 || imageBytes.length > MAX_IMAGE_BYTES) {
            throw badRequest("A foto deve ter no máximo 5 MB.");
        }

        try (ImageInputStream input = ImageIO.createImageInputStream(
                new ByteArrayInputStream(imageBytes))) {
            if (input == null) {
                throw badRequest("O arquivo não é uma imagem JPG ou PNG válida.");
            }

            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw badRequest("O arquivo não é uma imagem JPG ou PNG válida.");
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width < 1 || height < 1
                        || width > MAX_DIMENSION || height > MAX_DIMENSION) {
                    throw badRequest("A foto deve ter no máximo 1024x1024 pixels.");
                }

                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                boolean png = format.equals("png");
                boolean jpeg = format.equals("jpeg") || format.equals("jpg");
                if (!png && !jpeg) {
                    throw badRequest("A foto deve estar no formato JPG ou PNG.");
                }
                if ((png && !declaredMediaType.equals("data:image/png;base64"))
                        || (jpeg
                            && !declaredMediaType.equals("data:image/jpeg;base64")
                            && !declaredMediaType.equals("data:image/jpg;base64"))) {
                    throw badRequest("O formato informado não corresponde à imagem.");
                }

                BufferedImage image = reader.read(0);
                if (image == null) {
                    throw badRequest("Não foi possível ler a foto de perfil.");
                }
                image.flush();
                return png ? ".png" : ".jpg";
            } finally {
                reader.dispose();
            }
        } catch (IOException exception) {
            throw badRequest("Não foi possível ler a foto de perfil.");
        }
    }

    private String writeImage(byte[] imageBytes, String extension) {
        Path temporaryFile = null;
        try {
            Files.createDirectories(storageDirectory);
            temporaryFile = Files.createTempFile(
                storageDirectory,
                ".profile-picture-",
                ".tmp");
            Files.write(temporaryFile, imageBytes);

            String fileName = UUID.randomUUID() + extension;
            Path target = storageDirectory.resolve(fileName);
            try {
                Files.move(temporaryFile, target, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporaryFile, target);
            }
            return PUBLIC_PATH + fileName;
        } catch (IOException exception) {
            if (temporaryFile != null) {
                try {
                    Files.deleteIfExists(temporaryFile);
                } catch (IOException cleanupException) {
                    exception.addSuppressed(cleanupException);
                }
            }
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Não foi possível salvar a foto de perfil.",
                exception);
        }
    }

    private static String storedFileName(String picture) {
        if (picture == null) {
            return null;
        }
        var matcher = STORED_PATH.matcher(picture);
        return matcher.matches() ? matcher.group(1) : null;
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    public record StoredPicture(FileSystemResource resource, String contentType) {
    }
}
