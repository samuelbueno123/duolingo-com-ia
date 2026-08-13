import 'package:flutter/material.dart';

import 'login_page.dart';

class StudentLoginPage extends StatelessWidget {
  const StudentLoginPage({super.key});

  @override
  Widget build(BuildContext context) => const LoginPage(
    role: 'aluno(a)',
    primaryColor: Color(0xFF58CC02),
    icon: Icons.school_rounded,
  );
}
