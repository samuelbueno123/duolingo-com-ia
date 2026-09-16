# Duolingo / Duolinfo IA - Documentação para apresentação

## 1. Visão geral do projeto

Este repositório é um backend em Java com Spring Boot para uma plataforma de ensino de idiomas, com foco em dois perfis principais:

- Estudantes
- Professores

A ideia central da aplicação é permitir que um usuário se autentique com sua conta Google, tenha um perfil associado ao sistema e possa ter acesso a dados específicos conforme o tipo de perfil. O projeto também já foi pensado para funcionar com persistência em banco de dados, segurança por sessão, documentação automática com Swagger e execução em container Docker.

Em termos simples, a aplicação funciona como uma API REST que gerencia usuários, estudantes, professores, autenticação e dados de perfil, sendo a base para um frontend que possa consumir essas rotas para montar a interface da plataforma.

O projeto tem um caráter inicial/alpha, mais focado em estrutura e autenticação do que em regras de negócio avançadas de aula, progresso ou matchmaking. A parte principal que já existe é a base de identidade e perfil dos usuários.

---

## 2. Objetivo principal

O objetivo do sistema é:

- autenticar usuários via Google
- separar perfis em `STUDENT` e `TEACHER`
- armazenar informações de cada usuário
- permitir cadastro e consulta de estudantes e professores
- permitir buscas por nome, idioma, instituição, área de especialização, etc.
- preparar a base para uma experiência de plataforma de idiomas com foco em onboarding e gestão de perfis

---

## 3. Stack tecnológico

### Backend
- Java 17
- Spring Boot 3 / Spring Framework
- Maven
- Spring Web MVC
- Spring Data JPA
- Spring Security
- Spring Validation
- Hibernate / JPA

### Banco de dados
- H2 Database em desenvolvimento/local
- PostgreSQL como dependência para uso futuro/prod

### Autenticação
- Google OAuth / Google ID Token
- Biblioteca `google-api-client`
- Autenticação em sessão HTTP do Spring Security

### Documentação da API
- SpringDoc OpenAPI / Swagger UI

### Infraestrutura e deploy
- Docker
- Docker Compose
- Build e empacotamento de imagem via script PowerShell

### Testes
- Spring Boot Test
- MockMvc
- Spring Security Test
- Mockito

---

## 4. Arquitetura da aplicação

A aplicação segue o padrão MVC + camadas de serviço e repositório do Spring:

- `controller`: expõe endpoints HTTP
- `service`: contém regras de negócio e acesso ao repositório
- `repository`: interfaces JPA para persistência
- `entity`: classes de domínio/modelo do banco
- `config`: configurações de segurança, CORS, Swagger e verificação do token Google

### Estrutura principal

```text
src/
  main/
    java/
      com/
        duolinfo/
          ia/
            proj/
              ProjApplication.java
              config/
                CorsConfig.java
                GoogleTokenVerifier.java
                SecurityConfig.java
                SwaggerConfiguration.java
              controller/
                HomeController.java
                StudentController.java
                TeacherController.java
                UserController.java
                Auth/
                  AuthController.java
                  GoogleLoginRequest.java
                  GooglePayload.java
              entity/
                User.java
                Student.java
                Teacher.java
                StudentLanguage.java
                TeacherDocument.java
              repository/
                UserRepository.java
                StudentRepository.java
                TeacherRepository.java
              service/
                UserService.java
                StudentService.java
                TeacherService.java
    resources/
      application.properties
      application-docker.properties
  test/
    java/
      com/
        duolinfo/
          ia/
            proj/
              ProjApplicationTests.java
              SecurityAndAuthIntegrationTests.java
```

---

## 5. Classe principal e bootstrap

### `ProjApplication`

Arquivo principal do Spring Boot:

```java
@SpringBootApplication
public class ProjApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProjApplication.class, args);
    }
}
```

Ela inicia toda a aplicação e ativa a configuração automática do Spring Boot.

---

## 6. Configuração de segurança

### `SecurityConfig`

A segurança da API foi configurada com Spring Security e tem os seguintes atributos principais:

- habilita CORS
- desabilita CSRF
- usa sessão HTTP (`IF_REQUIRED`)
- permite requisições OPTIONS para evitar erro de preflight do navegador
- permite acesso público apenas para:
  - `/api/auth/google`
  - `/h2-console/**`
  - `/swagger-ui/**`
  - `/v3/api-docs/**`
  - recursos gerais da documentação
- exige autenticação para o restante das rotas
- customiza respostas 401 e 403 em JSON
- desativa o logout padrão do Spring e o controla no AuthController

### Política de autorização

A lógica da aplicação é:

- rota pública para login com Google
- rota pública para documentação e console H2
- resto do sistema protegido por autenticação

Isso é importante porque o backend não é anônimo: ele exige que o cliente tenha sessão autenticada para acessar dados de perfil e endpoints de gestão.

---

## 7. Autenticação com Google

### `GoogleTokenVerifier`

O projeto valida o token recebido do frontend usando a API do Google:

- usa a classe `GoogleIdTokenVerifier`
- verifica se o `audience` corresponde ao `google.client-id`
- valida o token recebido no login

### `AuthController`

A rota principal é:

```http
POST /api/auth/google
```

Ela recebe um payload como:

```json
{
  "credential": "token_do_google"
}
```

Fluxo da autenticação:

1. recebe a credencial do Google
2. chama `googleTokenVerifier.verify(credential)`
3. valida se email foi verificado pelo Google
4. extrai dados do payload: `googleId`, `email`, `name`, `picture`, `hd`
5. identifica o tipo de perfil (`TEACHER`, `STUDENT`, `USER`)
6. cria uma autenticação em Spring Security
7. salva o `SecurityContext` em sessão
8. retorna JSON com:
   - `success`
   - `message`
   - `user`
   - `profileType`

### Perfil do usuário

O `resolveProfileType` funciona assim:

- se existe professor com o mesmo `googleId` ou email → `TEACHER`
- se existe estudante com o mesmo `googleId` ou email → `STUDENT`
- caso contrário → `USER`

Isso permite que o sistema reconheça o perfil ao entrar no backend sem a necessidade de senha local.

### Sessão e logout

Também existem endpoints:

```http
GET /api/auth/me
POST /api/auth/logout
```

- `/api/auth/me` devolve se existe autenticação ativa
- `/api/auth/logout` invalida a sessão atual e limpa o contexto do Spring Security

---

## 8. Modelo de dados

### `User`

A entidade base do sistema é `User`.

Campos principais:

- `id`: identificador interno
- `googleId`: id do usuário no Google
- `name`: nome
- `email`: email único
- `profilePicture`: URL da imagem do perfil
- `passwordHash`: campo para hash de senha, apesar do login ser por Google

A classe usa:

- `@Entity`
- `@Table(name = "users")`
- `@Inheritance(strategy = InheritanceType.JOINED)`

Ou seja, `Student` e `Teacher` herdam de `User`, mas cada um é armazenado em tabela separada, mantendo a base comum e especializações próprias.

### `Student`

A entidade `Student` representa um aluno.

Campos/relacionamentos:

- `languages`: coleção de `StudentLanguage`

Relacionamento:

```java
@OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
private List<StudentLanguage> languages = new ArrayList<>();
```

Esse relacionamento permite que o aluno tenha múltiplos idiomas e níveis.

#### `StudentLanguage`

Representa o idioma que o aluno estuda.

Campos:

- `id`
- `languageName`: exemplo `Ingles`, `Espanhol`
- `level`: exemplo `1`, `2`, `A1`, `B2`, `S`
- `score`: pontuação acumulada
- `student`: referência ao estudante

Essa entidade é importante para modelar o progresso do aluno em idiomas.

### `Teacher`

A entidade `Teacher` representa um professor.

Campos principais:

- `institution`: instituição de ensino
- `taughtLanguages`: lista de idiomas lecionados
- `specializationAreas`: áreas de especialização
- `bibliography`: texto bibliográfico ou apresentação do professor
- `documents`: documentos relacionados ao professor

São usados `@ElementCollection` para listas de idiomas e áreas de especialização. Isso gera tabelas separadas para esse tipo de dado.

### `TeacherDocument`

Representa documentos do professor, como PDF ou Word.

Campos:

- `fileName`
- `contentType`
- `fileSize`
- `fileData`: conteúdo binário em `byte[]`
- `teacher`: professor dono do documento

Validações:

- tamanho máximo: 10MB
- tipos aceitos: PDF, DOCX, DOC

A entidade faz validação em `@PrePersist` e `@PreUpdate`.

---

## 9. Repositórios JPA

### `UserRepository`

Métodos importantes:

- `findByGoogleId(String googleId)`
- `findByEmail(String email)`

### `StudentRepository`

Métodos importantes:

- `findByGoogleId`
- `findByEmail`
- `findByLanguages_LanguageNameIgnoreCase(String languageName)`

Há também um método default:

```java
default List<Student> findByPreferredLanguage(String preferredLanguage) {
    return findByLanguages_LanguageNameIgnoreCase(preferredLanguage);
}
```

### `TeacherRepository`

Métodos importantes:

- `findByGoogleId`
- `findByEmail`
- `findByInstitutionContainingIgnoreCase`
- `findByTaughtLanguagesContainingIgnoreCase`
- `findBySpecializationAreasContainingIgnoreCase`

Esses métodos permitem busca por filtros de domínio, especialmente importantes para uma plataforma educacional.

---

## 10. Services

Os services encapsulam a lógica de acesso ao banco.

### `UserService`

Responsável por:

- criar usuários
- buscar todos
- buscar por id
- buscar por email ou googleId
- buscar por nome
- verificar existência
- deletar por id ou email

### `StudentService`

Responsável por:

- CRUD de estudante
- busca por nome
- busca por idioma preferido
- identificação por email ou Google

### `TeacherService`

Responsável por:

- CRUD de professor
- busca por nome
- busca por instituição
- busca por idioma ensinado
- busca por área de especialização
- validações por email e Google

---

## 11. Controllers e endpoints HTTP

## 11.1 `UserController`

Base: `/api/users`

Endpoints principais:

- `POST /api/users` → criar usuário
- `GET /api/users` → listar todos
- `GET /api/users/{id}` → buscar por ID
- `GET /api/users/google/{googleId}` → buscar por googleId
- `GET /api/users/email/{email}` → buscar por email
- `GET /api/users/name/{name}` → buscar por nome
- `PUT /api/users/{id}` → atualizar
- `DELETE /api/users/{id}` → deletar

### Observação

Esse controller existe, mas a segunda camada de perfil (`Student` e `Teacher`) é mais específica e mais alinhada ao domínio do projeto.

## 11.2 `StudentController`

Base: `/api/students`

Endpoints principais:

- `POST /api/students` → criar estudante
- `GET /api/students` → listar estudantes
- `GET /api/students/{id}` → buscar estudante por ID
- `GET /api/students/google/{googleId}` → buscar por googleId
- `GET /api/students/email/{email}` → buscar por email
- `GET /api/students/name/{name}` → buscar por nome
- `GET /api/students/language/{language}` → buscar estudantes por idioma
- `PUT /api/students/{id}` → atualizar
- `DELETE /api/students/{id}` → deletar

## 11.3 `TeacherController`

Base: `/api/teachers`

Endpoints principais:

- `POST /api/teachers` → criar professor
- `GET /api/teachers` → listar professores
- `GET /api/teachers/{id}` → buscar por ID
- `GET /api/teachers/google/{googleId}` → buscar por Google ID
- `GET /api/teachers/email/{email}` → buscar por email
- `GET /api/teachers/name/{name}` → buscar por nome
- `GET /api/teachers/institution/{institution}` → buscar por instituição
- `GET /api/teachers/language/{language}` → buscar por idioma lecionado
- `GET /api/teachers/specialization/{specializationArea}` → buscar por área de especialização
- `PUT /api/teachers/{id}` → atualizar
- `DELETE /api/teachers/{id}` → deletar

## 11.4 `HomeController`

Base: `/api/home`

Endpoint:

- `GET /api/home`

Resposta contém:

- `success`
- `user`
- `profileType`
- `profileCompleted`

Esse endpoint parece ter sido pensado para a tela inicial do sistema, indicando se o perfil foi completado ou não, diferenciando `USER` de `STUDENT` e `TEACHER`.

---

## 12. Configuração de CORS

### `CorsConfig`

O projeto define origens permitidas no CORS para permitir integração com frontend local:

- `http://127.0.0.1:5501`
- `http://localhost:5501`
- `http://127.0.0.1:5500`
- `http://localhost:5500`

Além disso, ele permite:

- GET
- POST
- PUT
- DELETE
- PATCH
- OPTIONS

Também configura `allowCredentials = true` para que cookies/sessões funcionem corretamente.

Essa configuração é crucial porque o frontend e o backend normalmente ficam em domínios/portas diferentes e o navegador bloqueia a comunicação sem CORS.

---

## 13. Configuração de banco e ambiente

### `application.properties`

Configurações principais:

- `server.port=8080`
- `spring.jpa.hibernate.ddl-auto=update`
- `spring.jpa.show-sql=true`
- H2 em arquivo local em `./data/duolingo`
- console H2 habilitado em `/h2-console`
- `google.client-id` configurado por variável de ambiente
- `spring.docker.compose.enabled=false`

### `application-docker.properties`

Versão específica para execução em container:

- usa H2 em `/app/data/duolingo`
- desativa console H2 em produção
- mantém porta 8080
- cookie de sessão definido para uso em Docker

---

## 14. Docker e deploy

### `docker-compose.yml`

O projeto possui um `docker-compose.yml` que:

- monta o backend em container
- expõe a porta 8080
- usa variável `GOOGLE_CLIENT_ID`
- usa variável `CORS_ALLOWED_ORIGINS`
- monta volume persistente `duolingo-data`

### `package-image.ps1`

Script que faz:

1. build da imagem Docker
2. criação do arquivo `.tar`

Exemplo:

```powershell
.\package-image.ps1
```

Esse fluxo é útil para exportar a imagem e carregar em outra máquina sem precisar rebuildar do zero.

---

## 15. Swagger / documentação da API

### `SwaggerConfiguration`

A API foi documentada com OpenAPI e ganha um nome e descrição específicos:

- título: `Duolinfo API`
- versão: `Alpha 0.0.3`
- descrição: `API para gerenciamento do Duolinfo.`

A documentação fica disponível em:

- `/swagger-ui/index.html`
- `/v3/api-docs`

Isso ajuda muito no desenvolvimento, testes e apresentação do backend para stakeholders.

---

## 16. Testes implementados

O projeto já conta com testes de integração de segurança e autenticação em `SecurityAndAuthIntegrationTests`.

### Exemplos de testes cobertos:

- bloqueio de `/api/home` sem autenticação
- bloqueio de estudantes/professores sem autenticação
- rejeição de login sem `credential`
- login Google bem-sucedido e criação de sessão
- consulta de `/api/auth/me`
- logout com invalidação da sessão
- acesso de usuário autenticado com tipo `STUDENT`
- acesso de usuário autenticado com tipo `TEACHER`
- verificação de que rotas antigas não são expostas

Esses testes demonstram a preocupação com boas práticas de segurança, autenticação e resposta HTTP correta.

---

## 17. Fluxo geral de uso do sistema

### Login do usuário

1. frontend coleta o token do Google
2. envia para `POST /api/auth/google`
3. backend valida token
4. cria autenticação em sessão
5. retorna principal + tipo de perfil
6. frontend usa sessão para navegar

### Perfil do usuário

- se o usuário for aluno → acesso a dados e endpoints relacionados a estudante
- se for professor → acesso a dados e endpoints de professor
- se não for identificado → perfil genérico `USER`

### Uso de dados iniciais

O frontend pode consultar:

```http
GET /api/home
```

para saber:

- quem é o usuário autenticado
- qual perfil está ativo
- se o perfil está completo

---

## 18. Padrões de design que aparecem no projeto

### 1. Camada de domínio
As entidades representam o negócio e o banco de dados.

### 2. Camada de persistência
Os repositórios usam JPA, abstraindo SQL e consultas.

### 3. Camada de serviço
Os services centralizam o acesso ao repositório e encapsulam a lógica de negócio.

### 4. Camada de controller
Os controllers expõem a REST API, definem endpoints e status HTTP.

### 5. Segurança via sessão
Em vez de JWT, o sistema usa contexto de segurança em sessão HTTP do Spring.

### 6. Abstração da autenticação Google
A validação do Google fica isolada em uma classe específica, o que facilita manutenção e testes.

---

## 19. Pontos fortes do projeto

- estrutura organizada em camadas
- uso de Spring Boot moderno
- autenticação com Google bem integrada
- suporte inicial à segregação de perfis
- JPA com modelagem clara de estudantes e professores
- documentação automática com Swagger
- integração pronta com Docker
- testes de segurança e sessão já adicionados
- banco local em H2 para desenvolvimento rápido

---

## 20. Pontos de atenção / limitações

Como é um backend inicial, ainda existem alguns aspectos que merecem destaque:

- não há autenticação por JWT customizada; usa sessão HTTP
- não há regras avançadas de aula, matrícula, progresso, pagamentos, chat ou IA
- não há frontend completo no repositório
- a gestão de documentos do professor existe, mas não há endpoints específicos de upload/download detalhados na leitura atual
- o projeto parece estar em evolução, mais focado em base de usuários e perfis do que em experiência completa de plataforma
- as entidades foram modeladas de maneira funcional, mas ainda não parecem cobrir todo o fluxo de um produto de ensino totalmente completo

Esses pontos podem ser usados em apresentação para mostrar que o sistema já está estruturado e que há espaço para expansão.

---

## 21. Potencial de apresentação para stakeholders

### Frase de apresentação (resumo executivo)

> Este projeto é um backend Java/Spring Boot para uma plataforma de ensino de idiomas, com autenticação via Google, gerenciamento de perfis de aluno e professor, persistência relacional e API REST documentada, com foco em base tecnológica para uma solução educacional escalável.

### Mensagem principal

- o sistema centraliza autenticação e perfis dos usuários
- oferece modelo para gestão de estudantes e professores
- mantém dados de idiomas, especialização e documentos
- cria base para expansão futura em IA, aulas, progresso e analytics

---

## 22. Roteiro sugerido para apresentação em slides

### Slide 1: Contexto e problema
- Plataforma de idiomas necessitando de gestão de usuários e perfis
- Necessidade de autenticação simples e segura

### Slide 2: Solução proposta
- Backend em Java/Spring Boot
- Login com Google
- Perfis separados para estudantes e professores

### Slide 3: Arquitetura do sistema
- Controller / Service / Repository / Entity / Config
- Estrutura em camadas

### Slide 4: Modelagem de dados
- Usuários base
- Estudantes e professores
- Idiomas, especialização e documentos

### Slide 5: Segurança e autenticação
- Spring Security
- Sessão HTTP
- CORS
- validação de token do Google

### Slide 6: API e endpoints
- exemplos de rotas para autenticação, home, aluno e professor

### Slide 7: Infraestrutura e deploy
- H2 local
- PostgreSQL em futuro/prod
- Docker e Docker Compose
- exportação de imagem via script

### Slide 8: Testes e qualidade
- integração de autenticação
- testes de sessão e acesso

### Slide 9: Próximos passos
- frontend
- gestão de aulas
- IA de recomendação
- progressão de aluno
- dashboards e relatórios

---

## 23. Comandos úteis para rodar o projeto

### Rodar localmente

```bash
./mvnw spring-boot:run
```

### Rodar com Docker Compose

```bash
docker compose up --build
```

### Build Maven

```bash
./mvnw clean install
```

### Ver documentação Swagger

```text
http://localhost:8080/swagger-ui/index.html
```

### Console H2

```text
http://localhost:8080/h2-console
```

---

## 24. Conclusão

Esse projeto é uma base sólida para uma plataforma de ensino de idiomas com foco em autenticação, perfis e gestão funcional. Ele demonstra domínio de Java, Spring Boot, JPA, segurança, autenticação externa, documentação de API e laboratórios de desenvolvimento com Docker.

Ele não é apenas um backend genérico: ele já foi pensado para o contexto de uma plataforma educacional com estudantes, professores, idiomas, especializações e documentos, sendo uma excelente base para apresentação de arquitetura, modelagem e solução de software educacional.

---

## 25. Resumo pronto para IA

Se você quiser usar este material como input para uma IA criar a apresentação, aqui vai um resumo bem enxuto:

> Backend de plataforma de idiomas em Java com Spring Boot. O sistema autentica usuários via Google, separa perfis de aluno e professor, armazena dados em JPA/Hibernate, usa Spring Security com sessão HTTP, oferece endpoints REST documentados em Swagger e suporta deploy com Docker. O banco principal em desenvolvimento é H2, com arquitetura preparada para futura expansão para PostgreSQL. O domínio inclui `User`, `Student`, `Teacher`, `StudentLanguage`, `TeacherDocument`, além de cadastro e busca por idioma, instituição e especialização. A aplicação está pronta como base para um produto educacional e já conta com testes de integração da camada de autenticação.

---

Se quiser, no próximo passo eu posso transformar esse material em:

1. uma apresentação em slides pronta em markdown,
2. um script de fala para apresentação,
3. uma versão mais comercial / mais técnica / mais acadêmica,
4. um resumo em 3-minutos para mostrar para professor ou cliente.
