import 'dart:async';
import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:google_sign_in/google_sign_in.dart';
import 'package:google_sign_in_web/web_only.dart' as google_web;
import 'package:http/http.dart' as http;

const _googleClientId =
    '367385027390-dmuj8neeqpgcbph3uk9qfb2cs5ubldhp.apps.googleusercontent.com';
const _backendUrl = String.fromEnvironment('BACKEND_URL', defaultValue: '');

String get _effectiveBackendUrl {
  if (_backendUrl.isNotEmpty) return _backendUrl;
  return '${Uri.base.scheme}://${Uri.base.host}:8080';
}

void main() {
  runApp(const DuolingoIaApp());
}

class DuolingoIaApp extends StatelessWidget {
  const DuolingoIaApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Duolingo com IA',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: const Color(0xFF58CC02)),
        useMaterial3: true,
      ),
      home: const GoogleLoginPage(),
    );
  }
}

class GoogleLoginPage extends StatefulWidget {
  const GoogleLoginPage({super.key});

  @override
  State<GoogleLoginPage> createState() => _GoogleLoginPageState();
}

class _GoogleLoginPageState extends State<GoogleLoginPage> {
  StreamSubscription<GoogleSignInAuthenticationEvent>? _authSubscription;
  GoogleUser? _user;
  String _status = 'Entre com sua conta Google para continuar.';
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _initializeGoogleSignIn();
  }

  Future<void> _initializeGoogleSignIn() async {
    try {
      _authSubscription = GoogleSignIn.instance.authenticationEvents.listen(
        _onAuthenticationEvent,
        onError: _onAuthenticationError,
      );

      await GoogleSignIn.instance.initialize(clientId: _googleClientId);
      GoogleSignIn.instance.attemptLightweightAuthentication();

      if (mounted) {
        setState(() => _loading = false);
      }
    } catch (_) {
      if (mounted) {
        setState(() {
          _loading = false;
          _status =
              'Não foi possível iniciar o login Google. Confira o Client ID e a URL cadastrada no Google Cloud.';
        });
      }
    }
  }

  Future<void> _onAuthenticationEvent(
    GoogleSignInAuthenticationEvent event,
  ) async {
    if (event is GoogleSignInAuthenticationEventSignOut) {
      if (mounted) {
        setState(() {
          _user = null;
          _status = 'Você saiu da conta.';
        });
      }
      return;
    }

    if (event is! GoogleSignInAuthenticationEventSignIn) return;

    final idToken = event.user.authentication.idToken;
    if (idToken == null || idToken.isEmpty) {
      _onAuthenticationError('O Google não retornou um ID Token.');
      return;
    }

    if (mounted) setState(() => _status = 'Validando login…');

    try {
      final response = await http.post(
        Uri.parse('$_effectiveBackendUrl/api/auth/google'),
        headers: const {'Content-Type': 'application/json'},
        body: jsonEncode({'credential': idToken}),
      );
      final body = jsonDecode(response.body) as Map<String, dynamic>;

      if (!response.isSuccessful || body['success'] != true) {
        throw StateError(body['message'] ?? 'O backend recusou o login.');
      }

      if (mounted) {
        setState(() {
          _user = GoogleUser.fromJson(body['user'] as Map<String, dynamic>);
          _status =
              body['message'] as String? ?? 'Login realizado com sucesso.';
        });
      }
    } catch (error) {
      _onAuthenticationError(
        'Falha ao chamar $_effectiveBackendUrl/api/auth/google. $error',
      );
    }
  }

  void _onAuthenticationError(Object error) {
    if (!mounted) return;
    setState(() {
      _user = null;
      _status = 'Não foi possível concluir o login: $error';
    });
  }

  Future<void> _signOut() async {
    await GoogleSignIn.instance.signOut();
  }

  @override
  void dispose() {
    _authSubscription?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF7F7F7),
      body: Center(
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 440),
          child: Card(
            elevation: 2,
            margin: const EdgeInsets.all(24),
            child: Padding(
              padding: const EdgeInsets.all(32),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  const Icon(
                    Icons.school_rounded,
                    color: Color(0xFF58CC02),
                    size: 56,
                  ),
                  const SizedBox(height: 16),
                  Text(
                    'Duolingo com IA',
                    textAlign: TextAlign.center,
                    style: Theme.of(context).textTheme.headlineSmall,
                  ),
                  const SizedBox(height: 8),
                  Text(_status, textAlign: TextAlign.center),
                  const SizedBox(height: 24),
                  if (_loading)
                    const Center(child: CircularProgressIndicator())
                  else if (_user == null)
                    Center(
                      child: google_web.renderButton(
                        configuration: google_web.GSIButtonConfiguration(
                          theme: google_web.GSIButtonTheme.outline,
                          size: google_web.GSIButtonSize.large,
                          text: google_web.GSIButtonText.signinWith,
                        ),
                      ),
                    )
                  else
                    _UserProfile(user: _user!, onSignOut: _signOut),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _UserProfile extends StatelessWidget {
  const _UserProfile({required this.user, required this.onSignOut});

  final GoogleUser user;
  final Future<void> Function() onSignOut;

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        if (user.picture != null)
          CircleAvatar(
            radius: 40,
            backgroundImage: NetworkImage(user.picture!),
          ),
        const SizedBox(height: 16),
        Text(
          user.name ?? 'Nome não informado',
          style: Theme.of(context).textTheme.titleLarge,
        ),
        const SizedBox(height: 4),
        Text(user.email ?? 'E-mail não informado'),
        const SizedBox(height: 20),
        OutlinedButton.icon(
          onPressed: onSignOut,
          icon: const Icon(Icons.logout),
          label: const Text('Sair'),
        ),
      ],
    );
  }
}

class GoogleUser {
  const GoogleUser({
    this.googleId,
    this.email,
    this.name,
    this.picture,
    this.hostedDomain,
  });

  factory GoogleUser.fromJson(Map<String, dynamic> json) => GoogleUser(
    googleId: json['googleId'] as String?,
    email: json['email'] as String?,
    name: json['name'] as String?,
    picture: json['picture'] as String?,
    hostedDomain: json['hostedDomain'] as String?,
  );

  final String? googleId;
  final String? email;
  final String? name;
  final String? picture;
  final String? hostedDomain;
}

extension on http.Response {
  bool get isSuccessful => statusCode >= 200 && statusCode < 300;
}
