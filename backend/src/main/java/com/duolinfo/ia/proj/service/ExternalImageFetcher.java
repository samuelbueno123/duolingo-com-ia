package com.duolinfo.ia.proj.service;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Iterator;
import java.util.Locale;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Baixa, valida (incluindo proteção SSRF e verificação de MIME/bytes) e
 * redimensiona imagens externas para URLs de foto de perfil seguras.
 */
final class ExternalImageFetcher {

    private static final int MAX_URL_LENGTH = 2048;
    private static final int MAX_REDIRECTS = 4;
    private static final int MAX_DOWNLOAD_BYTES = 6 * 1024 * 1024;
    private static final int MAX_DIMENSION = 1024;
    private static final int MAX_TARGET_BASE64_BYTES = 1_100_000;

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(8))
        .followRedirects(HttpClient.Redirect.NEVER)
        .build();

    private ExternalImageFetcher() {
    }

    record FetchedResult(String dataUrl, String verifiedExternalUrl) {
    }

    static FetchedResult fetchExternalImage(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw badRequest("Informe uma URL de imagem HTTP ou HTTPS válida.");
        }
        String trimmed = rawUrl.trim();
        if (trimmed.length() > MAX_URL_LENGTH) {
            throw badRequest("A URL da foto de perfil é muito longa.");
        }

        URI currentUri = parseAndValidatePublicUri(trimmed);

        try {
            HttpResponse<InputStream> response = sendFollowingSafeRedirects(currentUri);
            int status = response.statusCode();
            if (status < 200 || status >= 300) {
                try (InputStream ignored = response.body()) {
                    // Drain/close body cleanly.
                }
                throw badRequest(
                    "Não foi possível carregar a imagem dessa URL (HTTP "
                        + status
                        + "). Verifique se o link está público e acessível."
                );
            }

            String contentType = response.headers()
                .firstValue("Content-Type")
                .orElse("")
                .toLowerCase(Locale.ROOT);
            if (contentType.contains("text/html") || contentType.contains("application/xhtml")) {
                try (InputStream ignored = response.body()) {
                    // Drain/close body cleanly.
                }
                throw badRequest(
                    "Essa URL aponta para uma página web, e não para uma imagem direta. "
                        + "Clique com o botão direito na foto e escolha 'Copiar endereço da imagem'."
                );
            }

            byte[] rawBytes;
            try (InputStream in = response.body()) {
                rawBytes = readBounded(in, MAX_DOWNLOAD_BYTES);
            }
            if (rawBytes.length == 0) {
                throw badRequest("A URL informada retornou um arquivo vazio.");
            }
            if (looksLikeHtml(rawBytes)) {
                throw badRequest(
                    "Essa URL retornou uma página HTML em vez de uma imagem. "
                        + "Use o link direto do arquivo de imagem (.jpg, .png, .webp)."
                );
            }

            if (isWebp(rawBytes)) {
                return new FetchedResult(null, response.uri().toASCIIString());
            }

            BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(rawBytes));
            if (decoded == null || decoded.getWidth() <= 0 || decoded.getHeight() <= 0) {
                throw badRequest(
                    "O conteúdo retornado pela URL não é uma imagem válida (JPG, PNG, GIF ou WebP)."
                );
            }

            String normalizedDataUrl = encodeNormalizedDataUrl(decoded);
            return new FetchedResult(normalizedDataUrl, response.uri().toASCIIString());
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw badRequest("Tempo limite excedido ao baixar a imagem da URL.");
        } catch (IOException ex) {
            throw badRequest(
                "Não foi possível baixar a imagem da URL informada. Verifique o endereço e tente novamente."
            );
        }
    }

    private static HttpResponse<InputStream> sendFollowingSafeRedirects(URI initialUri)
            throws IOException, InterruptedException {
        URI uri = initialUri;
        for (int hop = 0; hop <= MAX_REDIRECTS; hop++) {
            HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(12))
                .header("User-Agent", "Mozilla/5.0 (compatible; DuolinfoMediaFetcher/1.0)")
                .header("Accept", "image/png,image/jpeg,image/gif,image/bmp;q=0.9,image/webp;q=0.7,*/*;q=0.1")
                .GET()
                .build();

            HttpResponse<InputStream> response = HTTP_CLIENT.send(
                request,
                HttpResponse.BodyHandlers.ofInputStream()
            );
            int code = response.statusCode();
            if (code == 301 || code == 302 || code == 303 || code == 307 || code == 308) {
                try (InputStream ignored = response.body()) {
                    // Close redirect response body before following.
                }
                String location = response.headers().firstValue("Location").orElse(null);
                if (location == null || location.isBlank()) {
                    throw badRequest("Redirecionamento inválido ao acessar a URL da imagem.");
                }
                URI resolved = uri.resolve(location.trim());
                uri = parseAndValidatePublicUri(resolved.toASCIIString());
                continue;
            }
            return response;
        }
        throw badRequest("Muitos redirecionamentos ao tentar carregar a imagem da URL.");
    }

    private static URI parseAndValidatePublicUri(String value) {
        final URI uri;
        try {
            uri = URI.create(value);
        } catch (IllegalArgumentException ex) {
            throw badRequest("Informe uma URL de imagem HTTP ou HTTPS válida.");
        }

        String scheme = uri.getScheme();
        String host = uri.getHost();
        if (scheme == null
                || (!scheme.equalsIgnoreCase("https") && !scheme.equalsIgnoreCase("http"))
                || host == null
                || host.isBlank()
                || uri.getUserInfo() != null) {
            throw badRequest("Informe uma URL de imagem HTTP ou HTTPS válida.");
        }

        String lowerHost = host.toLowerCase(Locale.ROOT);
        if (lowerHost.equals("localhost") || lowerHost.endsWith(".local") || lowerHost.endsWith(".internal")) {
            throw badRequest("URLs locais ou internas não são permitidas para foto de perfil.");
        }

        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            if (addresses.length == 0) {
                throw badRequest("Não foi possível resolver o endereço da URL informada.");
            }
            for (InetAddress address : addresses) {
                if (address.isAnyLocalAddress()
                        || address.isLoopbackAddress()
                        || address.isLinkLocalAddress()
                        || address.isSiteLocalAddress()
                        || address.isMulticastAddress()
                        || isCarrierOrUniqueLocal(address)) {
                    throw badRequest("O endereço informado aponta para uma rede privada e não pode ser acessado.");
                }
            }
        } catch (UnknownHostException ex) {
            throw badRequest("Não foi possível encontrar o servidor da URL informada.");
        }
        return uri;
    }

    private static boolean isCarrierOrUniqueLocal(InetAddress address) {
        byte[] raw = address.getAddress();
        if (raw.length == 4) {
            int b0 = raw[0] & 0xFF;
            int b1 = raw[1] & 0xFF;
            // 100.64.0.0/10 CGNAT or 0.0.0.0/8
            return b0 == 0 || (b0 == 100 && b1 >= 64 && b1 <= 127);
        }
        if (raw.length == 16) {
            int first = raw[0] & 0xFE;
            // fc00::/7 IPv6 ULA
            return first == 0xFC;
        }
        return false;
    }

    private static byte[] readBounded(InputStream in, int maxBytes) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int total = 0;
        int read;
        while ((read = in.read(buffer)) != -1) {
            total += read;
            if (total > maxBytes) {
                throw badRequest("A imagem da URL excede o tamanho máximo permitido (6 MB).");
            }
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    private static boolean looksLikeHtml(byte[] bytes) {
        int probeLen = Math.min(bytes.length, 256);
        String prefix = new String(bytes, 0, probeLen, StandardCharsets.UTF_8)
            .trim()
            .toLowerCase(Locale.ROOT);
        return prefix.startsWith("<!doctype html")
            || prefix.startsWith("<html")
            || prefix.startsWith("<head")
            || prefix.startsWith("<script")
            || prefix.startsWith("<?xml");
    }

    private static boolean isWebp(byte[] bytes) {
        return bytes.length >= 12
            && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
            && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
    }

    private static String encodeNormalizedDataUrl(BufferedImage source) throws IOException {
        BufferedImage resized = scaleDownIfNeeded(source, MAX_DIMENSION);
        boolean hasAlpha = resized.getColorModel() != null && resized.getColorModel().hasAlpha();

        if (hasAlpha) {
            byte[] pngBytes = writePng(resized);
            if (pngBytes.length <= MAX_TARGET_BASE64_BYTES) {
                return "data:image/png;base64," + Base64.getEncoder().encodeToString(pngBytes);
            }
        }

        BufferedImage rgb = toOpaqueRgb(resized);
        byte[] jpgBytes = writeJpeg(rgb, 0.88f);
        if (jpgBytes.length > MAX_TARGET_BASE64_BYTES) {
            BufferedImage smaller = scaleDownIfNeeded(rgb, 720);
            jpgBytes = writeJpeg(smaller, 0.78f);
        }
        if (jpgBytes.length > MAX_TARGET_BASE64_BYTES) {
            jpgBytes = writeJpeg(scaleDownIfNeeded(rgb, 512), 0.70f);
        }
        return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(jpgBytes);
    }

    private static BufferedImage scaleDownIfNeeded(BufferedImage image, int maxDim) {
        int width = image.getWidth();
        int height = image.getHeight();
        if (width <= maxDim && height <= maxDim) {
            return image;
        }
        double ratio = Math.min((double) maxDim / width, (double) maxDim / height);
        int targetW = Math.max(1, (int) Math.round(width * ratio));
        int targetH = Math.max(1, (int) Math.round(height * ratio));
        int type = (image.getColorModel() != null && image.getColorModel().hasAlpha())
            ? BufferedImage.TYPE_INT_ARGB
            : BufferedImage.TYPE_INT_RGB;
        BufferedImage canvas = new BufferedImage(targetW, targetH, type);
        Graphics2D g = canvas.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(image, 0, 0, targetW, targetH, null);
        } finally {
            g.dispose();
        }
        return canvas;
    }

    private static BufferedImage toOpaqueRgb(BufferedImage source) {
        if (source.getType() == BufferedImage.TYPE_INT_RGB) {
            return source;
        }
        BufferedImage rgb = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        try {
            g.setColor(java.awt.Color.WHITE);
            g.fillRect(0, 0, source.getWidth(), source.getHeight());
            g.drawImage(source, 0, 0, null);
        } finally {
            g.dispose();
        }
        return rgb;
    }

    private static byte[] writePng(BufferedImage image) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    private static byte[] writeJpeg(BufferedImage image, float quality) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            ImageIO.write(image, "jpg", out);
            return out.toByteArray();
        }
        ImageWriter writer = writers.next();
        try (MemoryCacheImageOutputStream stream = new MemoryCacheImageOutputStream(out)) {
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(quality);
            }
            writer.setOutput(stream);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
        return out.toByteArray();
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
