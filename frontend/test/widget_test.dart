import 'package:flutter_test/flutter_test.dart';

import 'package:frontend/main.dart';

void main() {
  testWidgets('shows the student and teacher access options', (tester) async {
    await tester.pumpWidget(const MyApp());

    expect(find.text('Área do aluno'), findsOneWidget);
    expect(find.text('Área do professor'), findsOneWidget);
  });
}
