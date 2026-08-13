import 'package:flutter/material.dart';

import 'login_page.dart';

class TeacherLoginPage extends StatelessWidget {
  const TeacherLoginPage({super.key});

  @override
  Widget build(BuildContext context) => const LoginPage(
    role: 'professor(a)',
    primaryColor: Color(0xFF1CB0F6),
    icon: Icons.co_present_rounded,
  );
}
