
/*

1. Entre no Microsoft Entra Admin Center
2. Vá em Entra ID
3. Registros de aplicativo
4. Novo registro
5. Dê um nome, por exemplo:
    PM-Painel


Em Tipos de conta com suporte, escolha:

    Contas em qualquer diretório organizacional e contas pessoais da Microsoft

Isso permite contas:

Microsoft 365 corporativas/escolares;
contas Microsoft pessoais (@outlook.com, @hotmail.com, etc.).

Se você quiser somente contas institucionais Microsoft 365, escolha:

Contas em qualquer diretório organizacional

A Microsoft documenta essas opções de audiência no registro do aplicativo.

Depois do registro

Você vai cair na página do aplicativo. Guarde principalmente:

Application (client) ID

Esse é o equivalente ao Client ID que você recebeu no Google.

Depois entre em:

Autenticação → Adicionar uma plataforma → Aplicativos móveis e de desktop

Para aplicativos desktop, a Microsoft possui uma configuração específica de Mobile and desktop applications e pode fornecer uma URI de redirecionamento apropriada, como:

https://login.microsoftonline.com/common/oauth2/nativeclient

Para o seu caso, isso é particularmente interessante porque você está fazendo um aplicativo Kivy desktop, e não um site tradicional.

E as permissões?

Depois vá em:

Permissões de API → Adicionar uma permissão → Microsoft Graph → Permissões delegadas

Para um login básico, normalmente você vai trabalhar com permissões como:

openid
profile
email
User.Read

O openid é usado para autenticação OpenID Connect, enquanto User.Read é a permissão básica para acessar informações do usuário pelo Microsoft Graph.

*/

const googleConfig = {
  clientId: "367385027390-dmuj8neeqpgcbph3uk9qfb2cs5ubldhp.apps.googleusercontent.com",
  backendUrl: "http://localhost:8080",
  backendLoginPath: "/api/auth/google"
};
