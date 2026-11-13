import 'dart:math' as math;

import 'package:coleta_seletiva_web/app/tema/tema.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:material_ui/material_ui.dart';

/// Envolve um widget com o tema e a localização do app.
Widget comTema(Widget filho, {double escalaTexto = 1}) => MaterialApp(
  theme: criarTema(),
  locale: const Locale('pt', 'BR'),
  supportedLocales: const [Locale('pt', 'BR')],
  localizationsDelegates: GlobalMaterialLocalizations.delegates,
  builder: (context, child) => MediaQuery(
    data: MediaQuery.of(context).copyWith(textScaler: TextScaler.linear(escalaTexto)),
    child: child!,
  ),
  home: filho,
);

/// Define a área lógica da janela de teste, como 360 × 800 ou 1366 × 768.
void definirTela(WidgetTester tester, Size tamanho) {
  tester.view.devicePixelRatio = 1;
  tester.view.physicalSize = tamanho;
  addTearDown(tester.view.reset);
}

const celular = Size(360, 800);
const computador = Size(1366, 768);

/// Contraste WCAG entre duas cores opacas.
double contraste(Color a, Color b) {
  double luminancia(Color c) {
    double canal(double v) => v <= 0.04045 ? v / 12.92 : math.pow((v + 0.055) / 1.055, 2.4).toDouble();
    return 0.2126 * canal(c.r) + 0.7152 * canal(c.g) + 0.0722 * canal(c.b);
  }

  final la = luminancia(a);
  final lb = luminancia(b);
  final (claro, escuro) = la > lb ? (la, lb) : (lb, la);
  return (claro + 0.05) / (escuro + 0.05);
}
