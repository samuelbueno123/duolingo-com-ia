import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:google_sign_in/google_sign_in.dart';
import 'package:http/http.dart' as http;
class LoginPage extends StatefulWidget {
  const LoginPage({
    super.key,
    required this.role,
    required this.primaryColor,
    required this.icon,
  });
  final String role;
  final Color primaryColor;
  final IconData icon;
  @override
  State<LoginPage> createState() => _LoginPageState();
}
class _LoginPageState extends State<LoginPage> {
  // TROQUE os valores abaixo pelos seus IDs/URLs reais do Google Cloud e do seu backend.
  // - `_googleWebClientId`: necessário para o login funcionar no Web.
  // - `_googleIosClientId`: necessário no iOS.
  // - `_googleServerClientId`: opcional, útil se seu backend validar o `idToken`.
  // - `_backendAuthExchangeUrl`: URL do seu backend para trocar/validar o token do Google.
  static const String _googleWebClientId =
      '367385027390-dmuj8neeqpgcbph3uk9qfb2cs5ubldhp.apps.googleusercontent.com';
  static const String _googleIosClientId =
      'COLOQUE_SEU_CLIENT_ID_IOS_AQUI.apps.googleusercontent.com';
  static const String _googleServerClientId =
      'COLOQUE_SEU_CLIENT_ID_SERVER_AQUI.apps.googleusercontent.com';
  static const String _backendAuthExchangeUrl =
      'https://SUA_URL_DO_BACKEND_AQUI/api/auth/google';
  final _formKey = GlobalKey<FormState>();
  late final GoogleSignIn _googleSignIn = GoogleSignIn(
    scopes: const <String>['email', 'profile'],
    clientId: kIsWeb
        ? (_hasWebClientIdConfigured ? _googleWebClientId : null)
        : (defaultTargetPlatform == TargetPlatform.iOS && _hasIosClientIdConfigured
            ? _googleIosClientId
            : null),
    serverClientId: _hasServerClientIdConfigured &&
            _googleServerClientId != _googleWebClientId
        ? _googleServerClientId
        : null,
  );
  GoogleSignInAccount? _googleAccount;
  GoogleSignInAuthentication? _googleAuth;
  bool _obscurePassword = true;
  bool _isGoogleSigningIn = false;
  DateTime? _signedInAt;
  String? _backendStatusMessage;
  bool get _hasWebClientIdConfigured =>
      !_googleWebClientId.contains('COLOQUE_SEU_CLIENT_ID_WEB_AQUI');

  bool get _hasIosClientIdConfigured =>
      !_googleIosClientId.contains('COLOQUE_SEU_CLIENT_ID_IOS_AQUI');

  bool get _hasServerClientIdConfigured =>
      !_googleServerClientId.contains('COLOQUE_SEU_CLIENT_ID_SERVER_AQUI');

  bool get _hasBackendUrlConfigured =>
      !_backendAuthExchangeUrl.contains('SUA_URL_DO_BACKEND_AQUI');
  @override
  void initState() {
    super.initState();
    _restoreGoogleSession();
  }
  void _showMessage(String message) {
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));
  }

  String _describeError(Object error, StackTrace stackTrace) {
    return '${error.toString()}\n\nStack trace:\n$stackTrace';
  }
  Future<void> _restoreGoogleSession() async {
    try {
      final account = await _googleSignIn.signInSilently();
      if (account == null) {
        return;
      }
      final auth = await account.authentication;
      if (!mounted) {
        return;
      }
      setState(() {
        _googleAccount = account;
        _googleAuth = auth;
        _signedInAt = DateTime.now();
      });
      try {
        final backendMessage = await _exchangeTokenWithBackend(account, auth);
        if (mounted) {
          setState(() => _backendStatusMessage = backendMessage);
        }
      } catch (error) {
        if (mounted) {
          setState(() {
            _backendStatusMessage =
                'Google conectado, mas o backend ainda não aceitou a troca do token.\n$error';
          });
        }
      }
    } catch (error) {
      if (mounted) {
        setState(() {
          _backendStatusMessage = 'Não foi possível restaurar a sessão: $error';
        });
      }
    }
  }
  Future<void> _handleGoogleSignIn() async {
    if (_isGoogleSigningIn) {
      return;
    }
    if (kIsWeb && !_hasWebClientIdConfigured) {
      _showMessage(
        'Troque `_googleWebClientId` pelo Client ID real do seu projeto no Google Cloud.',
      );
      return;
    }
    setState(() {
      _isGoogleSigningIn = true;
      _backendStatusMessage = null;
    });
    try {
      final account = await _googleSignIn.signIn();
      if (account == null) {
        if (mounted) {
          _showMessage('Login com Google cancelado.');
        }
        return;
      }
      final auth = await account.authentication;
      if (!mounted) {
        return;
      }
      setState(() {
        _googleAccount = account;
        _googleAuth = auth;
        _signedInAt = DateTime.now();
      });
      try {
        final backendMessage = await _exchangeTokenWithBackend(account, auth);
        if (mounted) {
          setState(() => _backendStatusMessage = backendMessage);
        }
      } catch (error) {
        if (mounted) {
          setState(() {
            _backendStatusMessage =
                'Login com Google realizado, mas a troca com o backend falhou.\n$error';
          });
        }
      }
      if (mounted) {
        _showMessage('Conectado como ${account.displayName ?? account.email}.');
      }
    } catch (error, stackTrace) {
      if (mounted) {
        setState(() {
          _backendStatusMessage =
              'Erro ao autenticar com Google:\n${_describeError(error, stackTrace)}';
        });
        _showMessage('Não foi possível entrar com Google. Veja o detalhe exibido na tela.');
      }
    } finally {
      if (mounted) {
        setState(() => _isGoogleSigningIn = false);
      }
    }
  }
  Future<void> _handleGoogleSignOut() async {
    await _googleSignIn.signOut();
    if (!mounted) {
      return;
    }
    setState(() {
      _googleAccount = null;
      _googleAuth = null;
      _signedInAt = null;
      _backendStatusMessage = null;
    });
    _showMessage('Conta do Google desconectada.');
  }
  Future<String> _exchangeTokenWithBackend(
    GoogleSignInAccount account,
    GoogleSignInAuthentication auth,
  ) async {
    if (!_hasBackendUrlConfigured) {
      return 'Conectado com Google. Troque `_backendAuthExchangeUrl` pela URL real da sua API para validar o token no servidor.';
    }
    final response = await http.post(
      Uri.parse(_backendAuthExchangeUrl),
      headers: const <String, String>{
        'Content-Type': 'application/json',
        'Accept': 'application/json',
      },
      body: jsonEncode(<String, dynamic>{
        'role': widget.role,
        'email': account.email,
        'displayName': account.displayName,
        'photoUrl': account.photoUrl,
        'googleId': account.id,
        'accessToken': auth.accessToken,
        'idToken': auth.idToken,
      }),
    );
    if (response.statusCode < 200 || response.statusCode >= 300) {
      throw StateError('Backend respondeu ${response.statusCode}: ${response.body}');
    }
    return 'Token do Google enviado ao backend com sucesso.';
  }
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        surfaceTintColor: Colors.transparent,
      ),
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.fromLTRB(24, 12, 24, 24),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 1160),
              child: LayoutBuilder(
                builder: (context, constraints) {
                  final isWide = constraints.maxWidth >= 900;
                  if (isWide) {
                    return Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Expanded(flex: 6, child: _buildLoginCard()),
                        const SizedBox(width: 24),
                        SizedBox(width: 360, child: _buildAccountCard()),
                      ],
                    );
                  }
                  return Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      _buildLoginCard(),
                      const SizedBox(height: 20),
                      _buildAccountCard(),
                    ],
                  );
                },
              ),
            ),
          ),
        ),
      ),
    );
  }
  Widget _buildLoginCard() {
    return Card(
      elevation: 0,
      color: Colors.white,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(24),
        side: const BorderSide(color: Color(0xFFE8E8E8)),
      ),
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Icon(widget.icon, color: widget.primaryColor, size: 64),
              const SizedBox(height: 20),
              Text(
                'Bem-vindo(a), ${widget.role}!',
                textAlign: TextAlign.center,
                style: const TextStyle(
                  fontSize: 28,
                  fontWeight: FontWeight.w800,
                ),
              ),
              const SizedBox(height: 8),
              const Text(
                'Faça login para continuar.',
                textAlign: TextAlign.center,
                style: TextStyle(color: Color(0xFF777777), fontSize: 16),
              ),
              const SizedBox(height: 32),
              TextFormField(
                keyboardType: TextInputType.emailAddress,
                decoration: _inputDecoration('Endereço de e-mail', Icons.email_outlined),
                validator: (value) =>
                    value == null || !value.contains('@')
                        ? 'Digite um endereço de e-mail válido.'
                        : null,
              ),
              const SizedBox(height: 14),
              TextFormField(
                obscureText: _obscurePassword,
                decoration: _inputDecoration('Senha', Icons.lock_outline).copyWith(
                  suffixIcon: IconButton(
                    icon: Icon(
                      _obscurePassword
                          ? Icons.visibility_outlined
                          : Icons.visibility_off_outlined,
                    ),
                    onPressed: () => setState(
                      () => _obscurePassword = !_obscurePassword,
                    ),
                  ),
                ),
                validator: (value) => value == null || value.length < 6
                    ? 'A senha deve ter pelo menos 6 caracteres.'
                    : null,
              ),
              Align(
                alignment: Alignment.centerRight,
                child: TextButton(
                  onPressed: () => _showMessage(
                    'Recuperação de senha disponível em breve.',
                  ),
                  child: Text(
                    'Esqueceu sua senha?',
                    style: TextStyle(color: widget.primaryColor),
                  ),
                ),
              ),
              const SizedBox(height: 8),
              SizedBox(
                height: 52,
                child: FilledButton(
                  style: FilledButton.styleFrom(
                    backgroundColor: widget.primaryColor,
                  ),
                  onPressed: () {
                    if (_formKey.currentState!.validate()) {
                      _showMessage(
                        'Login local validado. O botão do Google está funcionando de verdade.',
                      );
                    }
                  },
                  child: const Text(
                    'FAZER LOGIN',
                    style: TextStyle(fontWeight: FontWeight.bold),
                  ),
                ),
              ),
              const Padding(
                padding: EdgeInsets.symmetric(vertical: 24),
                child: Row(
                  children: [
                    Expanded(child: Divider()),
                    Padding(
                      padding: EdgeInsets.symmetric(horizontal: 12),
                      child: Text(
                        'OU',
                        style: TextStyle(color: Color(0xFF777777)),
                      ),
                    ),
                    Expanded(child: Divider()),
                  ],
                ),
              ),
              SizedBox(
                height: 52,
                child: OutlinedButton.icon(
                  onPressed: _isGoogleSigningIn ? null : _handleGoogleSignIn,
                  icon: _isGoogleSigningIn
                      ? const SizedBox(
                          width: 18,
                          height: 18,
                          child: CircularProgressIndicator(strokeWidth: 2),
                        )
                      : const Text(
                          'G',
                          style: TextStyle(
                            fontSize: 20,
                            fontWeight: FontWeight.w700,
                          ),
                        ),
                  label: Text(
                    _isGoogleSigningIn ? 'CONECTANDO...' : 'CONTINUAR COM GOOGLE',
                    style: const TextStyle(fontWeight: FontWeight.bold),
                  ),
                ),
              ),
              const SizedBox(height: 26),
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  const Text('Novo aqui?'),
                  TextButton(
                    onPressed: () => _showMessage(
                      'O cadastro estará disponível em breve.',
                    ),
                    child: Text(
                      'Cadastrar',
                      style: TextStyle(color: widget.primaryColor),
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
  Widget _buildAccountCard() {
    final account = _googleAccount;
    return Card(
      elevation: 0,
      color: widget.primaryColor.withValues(alpha: 0.06),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(24),
        side: BorderSide(color: widget.primaryColor.withValues(alpha: 0.22)),
      ),
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Row(
              children: [
                Expanded(
                  child: Text(
                    'Conta logada',
                    style: TextStyle(
                      fontSize: 22,
                      fontWeight: FontWeight.w800,
                      color: widget.primaryColor,
                    ),
                  ),
                ),
                Chip(
                  label: const Text('Google'),
                  labelStyle: TextStyle(
                    color: widget.primaryColor,
                    fontWeight: FontWeight.w700,
                  ),
                  backgroundColor: Colors.white,
                  side: BorderSide(
                    color: widget.primaryColor.withValues(alpha: 0.25),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 16),
            if (account == null) ...[
              const Icon(
                Icons.manage_accounts_outlined,
                size: 68,
                color: Color(0xFF8A8A8A),
              ),
              const SizedBox(height: 16),
              const Text(
                'Sua conta aparecerá aqui depois que você entrar com o Google.',
                textAlign: TextAlign.center,
                style: TextStyle(fontSize: 16, color: Color(0xFF666666)),
              ),
              const SizedBox(height: 12),
              const Text(
                'Configure os placeholders no código para apontar para o seu Client ID e para a sua URL de backend.',
                textAlign: TextAlign.center,
                style: TextStyle(color: Color(0xFF7A7A7A)),
              ),
            ] else ...[
              Row(
                children: [
                  _AccountAvatar(account: account, color: widget.primaryColor),
                  const SizedBox(width: 14),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          account.displayName?.trim().isNotEmpty == true
                              ? account.displayName!
                              : 'Usuário do Google',
                          style: const TextStyle(
                            fontSize: 20,
                            fontWeight: FontWeight.w800,
                          ),
                        ),
                        const SizedBox(height: 4),
                        Text(
                          account.email,
                          style: const TextStyle(
                            color: Color(0xFF666666),
                            fontSize: 15,
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 18),
              _InfoRow(label: 'ID do Google', value: account.id),
              const SizedBox(height: 10),
              _InfoRow(
                label: 'Foto',
                value: account.photoUrl ?? 'Sem foto de perfil',
              ),
              const SizedBox(height: 10),
              _InfoRow(
                label: 'Autenticado em',
                value: _signedInAt == null
                    ? 'Agora'
                    : '${_signedInAt!.day.toString().padLeft(2, '0')}/${_signedInAt!.month.toString().padLeft(2, '0')}/${_signedInAt!.year} ${_signedInAt!.hour.toString().padLeft(2, '0')}:${_signedInAt!.minute.toString().padLeft(2, '0')}',
              ),
              const SizedBox(height: 10),
              _InfoRow(
                label: 'Token do Google',
                value: (_googleAuth?.idToken != null ||
                        _googleAuth?.accessToken != null)
                    ? 'Recebido e pronto para enviar ao backend'
                    : 'Ainda não disponível',
              ),
              if (_backendStatusMessage != null) ...[
                const SizedBox(height: 16),
                Container(
                  width: double.infinity,
                  padding: const EdgeInsets.all(14),
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(16),
                    border: Border.all(
                      color: widget.primaryColor.withValues(alpha: 0.18),
                    ),
                  ),
                  child: Text(
                    _backendStatusMessage!,
                    style: const TextStyle(color: Color(0xFF444444)),
                  ),
                ),
              ],
              const SizedBox(height: 18),
              SizedBox(
                height: 48,
                child: FilledButton.tonal(
                  style: FilledButton.styleFrom(
                    foregroundColor: widget.primaryColor,
                  ),
                  onPressed: _handleGoogleSignOut,
                  child: const Text(
                    'Sair da conta',
                    style: TextStyle(fontWeight: FontWeight.bold),
                  ),
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }
  InputDecoration _inputDecoration(String label, IconData icon) =>
      InputDecoration(
        labelText: label,
        prefixIcon: Icon(icon, color: widget.primaryColor),
        border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: const BorderSide(color: Color(0xFFD7D7D7)),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: BorderSide(color: widget.primaryColor, width: 2),
        ),
      );
}
class _AccountAvatar extends StatelessWidget {
  const _AccountAvatar({required this.account, required this.color});
  final GoogleSignInAccount account;
  final Color color;
  @override
  Widget build(BuildContext context) {
    final photoUrl = account.photoUrl;
    final initials = _buildInitials(account.displayName, account.email);
    return CircleAvatar(
      radius: 34,
      backgroundColor: color.withValues(alpha: 0.14),
      backgroundImage: photoUrl == null ? null : NetworkImage(photoUrl),
      child: photoUrl == null
          ? Text(
              initials,
              style: TextStyle(
                color: color,
                fontWeight: FontWeight.w800,
                fontSize: 18,
              ),
            )
          : null,
    );
  }
  String _buildInitials(String? displayName, String email) {
    final name = displayName?.trim();
    if (name != null && name.isNotEmpty) {
      final parts = name.split(RegExp(r'\s+'));
      final first = parts.first.isNotEmpty ? parts.first[0] : '';
      final last = parts.length > 1 && parts.last.isNotEmpty ? parts.last[0] : '';
      final initials = '$first$last'.trim();
      if (initials.isNotEmpty) {
        return initials.toUpperCase();
      }
    }
    return email.isNotEmpty ? email[0].toUpperCase() : 'U';
  }
}
class _InfoRow extends StatelessWidget {
  const _InfoRow({required this.label, required this.value});
  final String label;
  final String value;
  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          label,
          style: const TextStyle(
            fontSize: 12,
            fontWeight: FontWeight.w700,
            color: Color(0xFF777777),
          ),
        ),
        const SizedBox(height: 4),
        Text(
          value,
          style: const TextStyle(fontSize: 15, fontWeight: FontWeight.w600),
        ),
      ],
    );
  }
}
