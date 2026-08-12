// ============================================================
// CONFIGURAÇÃO
// ============================================================

const BACKEND_URL = googleConfig.backendUrl;

const GOOGLE_LOGIN_URL =
    `${BACKEND_URL}/api/auth/google`;


// ============================================================
// ELEMENTOS DA PÁGINA
// ============================================================

const statusElement =
    document.getElementById("status");

const userInfo =
    document.getElementById("userInfo");

const userName =
    document.getElementById("userName");

const userEmail =
    document.getElementById("userEmail");

const userGoogleId =
    document.getElementById("userGoogleId");

const userHostedDomain =
    document.getElementById("userHostedDomain");

const userPicture =
    document.getElementById("userPicture");


// ============================================================
// CALLBACK DO GOOGLE
// ============================================================
//
// O Google Identity Services chama automaticamente esta
// função depois que o usuário termina o login.
//
// credentialResponse.credential contém o ID Token.
// ============================================================

async function handleGoogleCredential(
    credentialResponse
) {

    console.log(
        "Credencial recebida do Google."
    );


    // ========================================================
    // VERIFICAR SE O GOOGLE ENVIOU O TOKEN
    // ========================================================

    if (
        !credentialResponse ||
        !credentialResponse.credential
    ) {

        console.error(
            "O Google não retornou uma credencial."
        );

        setStatus(
            "O Google não retornou o token.",
            "error"
        );

        return;
    }


    const credential =
        credentialResponse.credential;


    // ========================================================
    // DEBUG
    // ========================================================
    //
    // NÃO mostramos o token inteiro no console.
    //
    // O ID Token é uma credencial sensível kkk.
    // ========================================================

    console.log(
        "ID Token recebido."
    );


    // ========================================================
    // ENVIAR TOKEN PARA O SPRING BOOT PARA VALIDAR
    // ========================================================

    try {

        setStatus(
            "Validando login...",
            "loading"
        );


        const response =
            await fetch( // fetch em js é usado para fazer as requisições GET,POST e etc
                GOOGLE_LOGIN_URL, // URL para onde vai mandar a requisição
                {
                    method: "POST", // Método usado, dah kkkk

                    headers: { // Cabeçalho da requisição
                        "Content-Type":
                            "application/json"
                    },

                    body: JSON.stringify({// Corpo da requisição com a credencial de validação
                        credential: credential // Credencial
                    })
                }
            );


        // ====================================================
        // LER RESPOSTA
        // ====================================================

        const data =
            await response.json(); // Transforma a resposta em JSON, dah kkk


        console.log(
            "Resposta do backend:",
            data
        );


        // ====================================================
        // LOGIN RECUSADO
        // ====================================================

        if (!response.ok || !data.success) {

            console.error(
                "Backend recusou o login:",
                data
            );

            setStatus(
                data.message ||
                "Não foi possível realizar o login.",
                "error"
            );

            return;
        }


        // ====================================================
        // LOGIN BEM-SUCEDIDO
        // ====================================================

        console.log(
            "Login realizado com sucesso."
        );


        showUser(
            data.user
        );


    } catch (error) {

        // ====================================================
        // ERRO DE CONEXÃO
        // ====================================================

        console.error(
            "Erro ao comunicar com o backend:",
            error
        );


        setStatus(
            "Não foi possível conectar ao servidor.",
            "error"
        );
    }
}


// ============================================================
// MOSTRAR USUÁRIO
// ============================================================

function showUser(user) {

    if (!user) {

        setStatus(
            "O backend não retornou os dados do usuário.",
            "error"
        );

        return;
    }


    // ========================================================
    // STATUS
    // ========================================================

    setStatus(
        "Login realizado com sucesso!",
        "success"
    );

    // ========================================================
    // DAQUI PARA BAIXO VC PODE PEGAR A REQUISIÇÂO E MANDAR 
    // PARA O SPRING CRIAR OU ENTRAR NA CONTA DO USUÀRIO
    // ========================================================


    // ========================================================
    // MOSTRAR ÁREA DO USUÁRIO
    // ========================================================

    userInfo.hidden = false;


    // ========================================================
    // NOME
    // ========================================================

    userName.textContent =
        user.name ||
        "Não informado";


    // ========================================================
    // EMAIL
    // ========================================================

    userEmail.textContent =
        user.email ||
        "Não informado";


    // ========================================================
    // GOOGLE ID
    // ========================================================

    userGoogleId.textContent =
        user.googleId ||
        "Não informado";


    // ========================================================
    // DOMÍNIO
    // ========================================================

    userHostedDomain.textContent =
        user.hostedDomain ||
        "Conta pessoal";


    // ========================================================
    // FOTO
    // ========================================================

    if (user.picture) {

        userPicture.src =
            user.picture;

        userPicture.hidden = false;

    } else {

        userPicture.hidden = true;
    }
}


// ============================================================
// ALTERAR STATUS
// ============================================================

function setStatus(
    message,
    type = "normal"
) {

    statusElement.textContent =
        message;


    statusElement.className =
        "status";


    if (type === "success") {

        statusElement.classList.add(
            "status-success"
        );

    } else if (type === "error") {

        statusElement.classList.add(
            "status-error"
        );

    } else if (type === "loading") {

        statusElement.classList.add(
            "status-loading"
        );
    }
}
