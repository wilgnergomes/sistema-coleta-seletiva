import 'package:material_ui/material_ui.dart';

/// Tokens do design system "Minimalismo Funcional Sustentável".
abstract final class Cores {
  static const primario = Color(0xFF176B4D);
  static const primarioHover = Color(0xFF13583F);
  static const primarioPressionado = Color(0xFF104B36);
  static const sobrePrimario = Color(0xFFFFFFFF);
  static const secundarioSuave = Color(0xFF83B89C);
  static const fundo = Color(0xFFF7F9F7);
  static const fundoVerde = Color(0xFFEEF5F0);
  static const superficie = Color(0xFFFFFFFF);
  static const texto = Color(0xFF24352D);
  static const textoSecundario = Color(0xFF52665B);
  static const contorno = Color(0xFF73857A);
  static const divisor = Color(0xFFDCE4DE);
  static const destaque = Color(0xFFE9AD55);
  static const foco = Color(0xFF1C5FB8);
  static const desabilitadoFundo = Color(0xFFE9EEEA);
  static const sucesso = Color(0xFF176044);
  static const sucessoFundo = Color(0xFFDDF3E8);
  static const alerta = Color(0xFF694400);
  static const alertaFundo = Color(0xFFFFF1D6);
  static const erro = Color(0xFF922E2C);
  static const erroHover = Color(0xFF7C2523);
  static const erroPressionado = Color(0xFF661E1C);
  static const erroFundo = Color(0xFFFCE8E7);
  static const neutro = Color(0xFF4C5850);
  static const neutroFundo = Color(0xFFE9EEEA);
}

/// Escala de espaçamento em unidades lógicas.
abstract final class Espaco {
  static const xxs = 4.0;
  static const xs = 8.0;
  static const s = 12.0;
  static const m = 16.0;
  static const l = 24.0;
  static const xl = 32.0;
  static const xxl = 48.0;
}

abstract final class Raio {
  static const campo = 8.0;
  static const cartao = 12.0;
  static const dialogo = 16.0;
  static const pilula = 999.0;
}

/// Larguras máximas de leitura.
abstract final class Largura {
  static const formulario = 720.0;
  static const catalogo = 1120.0;
  static const densa = 1280.0;
  static const dialogo = 520.0;
}

/// Limiares de navegação do shell, a partir da largura útil.
abstract final class Limiar {
  static const intermediario = 600.0;
  static const amplo = 1000.0;
}

/// Margem lateral conforme a largura disponível.
double margemLateral(double largura) => largura < Limiar.intermediario ? Espaco.m : Espaco.l;
