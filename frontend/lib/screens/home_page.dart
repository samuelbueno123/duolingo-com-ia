import 'package:flutter/material.dart';

import 'student_login_page.dart';
import 'teacher_login_page.dart';

class HomePage extends StatelessWidget {
  const HomePage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 540),
              child: Column(
                children: [
                  const Icon(
                    Icons.menu_book_rounded,
                    color: Color(0xFF58CC02),
                    size: 70,
                  ),
                  const SizedBox(height: 18),
                  const Text(
                    'Duolinfo',
                    textAlign: TextAlign.center,
                    style: TextStyle(fontSize: 30, fontWeight: FontWeight.w800),
                  ),
                  const SizedBox(height: 8),
                  const Text(
                    'Escolha como deseja acessar a plataforma.',
                    textAlign: TextAlign.center,
                    style: TextStyle(color: Color(0xFF777777), fontSize: 16),
                  ),
                  const SizedBox(height: 36),
                  _AccessCard(
                    title: 'Área do aluno',
                    description: 'Aprenda, pratique e acompanhe sua evolução.',
                    buttonText: 'ENTRAR COMO ALUNO',
                    color: const Color(0xFF58CC02),
                    icon: Icons.school_rounded,
                    onPressed: () => Navigator.of(context).push(
                      MaterialPageRoute(
                        builder: (_) => const StudentLoginPage(),
                      ),
                    ),
                  ),
                  const SizedBox(height: 18),
                  _AccessCard(
                    title: 'Área do professor',
                    description: 'Gerencie turmas e acompanhe seus estudantes.',
                    buttonText: 'ENTRAR COMO PROFESSOR',
                    color: const Color(0xFF1CB0F6),
                    icon: Icons.co_present_rounded,
                    onPressed: () => Navigator.of(context).push(
                      MaterialPageRoute(
                        builder: (_) => const TeacherLoginPage(),
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _AccessCard extends StatelessWidget {
  const _AccessCard({
    required this.title,
    required this.description,
    required this.buttonText,
    required this.color,
    required this.icon,
    required this.onPressed,
  });

  final String title;
  final String description;
  final String buttonText;
  final Color color;
  final IconData icon;
  final VoidCallback onPressed;

  @override
  Widget build(BuildContext context) {
    return Card(
      elevation: 0,
      color: color.withValues(alpha: 0.08),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(20),
        side: BorderSide(color: color.withValues(alpha: 0.35)),
      ),
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          children: [
            Icon(icon, color: color, size: 48),
            const SizedBox(height: 12),
            Text(
              title,
              style: const TextStyle(fontSize: 22, fontWeight: FontWeight.w800),
            ),
            const SizedBox(height: 6),
            Text(description, textAlign: TextAlign.center),
            const SizedBox(height: 20),
            SizedBox(
              width: double.infinity,
              height: 50,
              child: FilledButton(
                style: FilledButton.styleFrom(backgroundColor: color),
                onPressed: onPressed,
                child: Text(
                  buttonText,
                  style: const TextStyle(fontWeight: FontWeight.bold),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
