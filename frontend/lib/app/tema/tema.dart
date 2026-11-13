import 'package:material_ui/material_ui.dart';

import '../../core/dominio/enums.dart';
import 'tokens.dart';

/// Cor, fundo e ícone de uma situação. Rótulo e ícone sempre acompanham a cor.
class VisualSituacao {
  const VisualSituacao(this.cor, this.fundo, this.icone);

  final Color cor;
  final Color fundo;
  final IconData icone;
}

/// Papéis de situação, fora do `ColorScheme` (design system, seção Situações).
class CoresSituacao extends ThemeExtension<CoresSituacao> {
  const CoresSituacao();

  static const _solicitacao = {
    SituacaoSolicitacao.solicitada: VisualSituacao(Color(0xFF304E67), Color(0xFFE8F0F8), Icons.schedule),
    SituacaoSolicitacao.agendada: VisualSituacao(Color(0xFF165670), Color(0xFFDDF1F8), Icons.event),
    SituacaoSolicitacao.emRecolhimento: VisualSituacao(
      Color(0xFF694400),
      Color(0xFFFFF1D6),
      Icons.local_shipping_outlined,
    ),
    SituacaoSolicitacao.concluida: VisualSituacao(Cores.sucesso, Cores.sucessoFundo, Icons.check_circle_outline),
    SituacaoSolicitacao.recusada: VisualSituacao(Cores.erro, Cores.erroFundo, Icons.block),
    SituacaoSolicitacao.cancelada: VisualSituacao(Cores.neutro, Cores.neutroFundo, Icons.cancel_outlined),
    SituacaoSolicitacao.desconhecida: VisualSituacao(Cores.neutro, Cores.neutroFundo, Icons.help_outline),
  };

  VisualSituacao solicitacao(SituacaoSolicitacao situacao) => _solicitacao[situacao]!;

  VisualSituacao cadastro(SituacaoCadastro situacao) => switch (situacao) {
    SituacaoCadastro.ativo => const VisualSituacao(Cores.sucesso, Cores.sucessoFundo, Icons.check_circle_outline),
    SituacaoCadastro.inativo => const VisualSituacao(Cores.neutro, Cores.neutroFundo, Icons.pause_circle_outline),
    SituacaoCadastro.desconhecida => const VisualSituacao(Cores.neutro, Cores.neutroFundo, Icons.help_outline),
  };

  @override
  CoresSituacao copyWith() => this;

  @override
  CoresSituacao lerp(CoresSituacao? other, double t) => this;
}

extension TemaApp on BuildContext {
  CoresSituacao get coresSituacao => Theme.of(this).extension<CoresSituacao>() ?? const CoresSituacao();
}

WidgetStateProperty<BorderSide?> _anelFoco([BorderSide? padrao]) => WidgetStateProperty.resolveWith(
  (estados) => estados.contains(WidgetState.focused) ? const BorderSide(color: Cores.foco, width: 2) : padrao,
);

const _fonte = 'Inter';

TextTheme _texto() {
  TextStyle estilo(double tamanho, double linha, FontWeight peso) => TextStyle(
    fontFamily: _fonte,
    fontSize: tamanho,
    height: linha / tamanho,
    fontWeight: peso,
    color: Cores.texto,
    letterSpacing: 0,
  );
  const regular = FontWeight.w400;
  const semi = FontWeight.w600;
  return TextTheme(
    displaySmall: estilo(32, 40, semi),
    headlineMedium: estilo(28, 36, semi),
    headlineSmall: estilo(24, 32, semi),
    titleLarge: estilo(20, 28, semi),
    titleMedium: estilo(16, 24, semi),
    titleSmall: estilo(14, 20, semi),
    bodyLarge: estilo(16, 24, regular),
    bodyMedium: estilo(14, 20, regular),
    bodySmall: estilo(12, 16, regular).copyWith(color: Cores.textoSecundario),
    labelLarge: estilo(14, 20, semi),
    labelMedium: estilo(12, 16, semi),
    labelSmall: estilo(12, 16, regular),
  );
}

ThemeData criarTema() {
  final esquema = ColorScheme.fromSeed(seedColor: Cores.primario, dynamicSchemeVariant: DynamicSchemeVariant.fidelity)
      .copyWith(
        primary: Cores.primario,
        onPrimary: Cores.sobrePrimario,
        primaryContainer: Cores.sucessoFundo,
        onPrimaryContainer: Cores.texto,
        secondary: Cores.primario,
        onSecondary: Cores.sobrePrimario,
        secondaryContainer: Cores.sucessoFundo,
        onSecondaryContainer: Cores.texto,
        tertiary: Cores.destaque,
        onTertiary: Cores.texto,
        surface: Cores.superficie,
        onSurface: Cores.texto,
        onSurfaceVariant: Cores.textoSecundario,
        surfaceContainerLowest: Cores.superficie,
        surfaceContainerLow: Cores.fundo,
        surfaceContainer: Cores.fundoVerde,
        surfaceContainerHigh: Cores.fundoVerde,
        surfaceContainerHighest: Cores.neutroFundo,
        outline: Cores.contorno,
        outlineVariant: Cores.divisor,
        error: Cores.erro,
        onError: Cores.sobrePrimario,
        errorContainer: Cores.erroFundo,
        onErrorContainer: Cores.erro,
      );
  final texto = _texto();
  final formaCampo = RoundedRectangleBorder(borderRadius: BorderRadius.circular(Raio.campo));
  const tamanhoBotao = Size(64, 48);
  final rotuloBotao = WidgetStatePropertyAll(texto.labelLarge);

  OutlineInputBorder borda(Color cor, [double largura = 1]) => OutlineInputBorder(
    borderRadius: BorderRadius.circular(Raio.campo),
    borderSide: BorderSide(color: cor, width: largura),
  );

  return ThemeData(
    useMaterial3: true,
    colorScheme: esquema,
    fontFamily: _fonte,
    textTheme: texto,
    scaffoldBackgroundColor: Cores.fundo,
    focusColor: Cores.foco.withValues(alpha: 0.12),
    visualDensity: VisualDensity.standard,
    materialTapTargetSize: MaterialTapTargetSize.padded,
    extensions: const [CoresSituacao()],
    appBarTheme: AppBarTheme(
      backgroundColor: Cores.superficie,
      foregroundColor: Cores.texto,
      surfaceTintColor: Colors.transparent,
      elevation: 0,
      scrolledUnderElevation: 1,
      titleTextStyle: texto.titleLarge,
      shape: const Border(bottom: BorderSide(color: Cores.divisor)),
    ),
    filledButtonTheme: FilledButtonThemeData(
      style: ButtonStyle(
        minimumSize: const WidgetStatePropertyAll(tamanhoBotao),
        shape: WidgetStatePropertyAll(formaCampo),
        textStyle: rotuloBotao,
        side: _anelFoco(),
        backgroundColor: WidgetStateProperty.resolveWith(
          (e) => e.contains(WidgetState.disabled)
              ? Cores.desabilitadoFundo
              : e.contains(WidgetState.pressed)
              ? Cores.primarioPressionado
              : e.contains(WidgetState.hovered)
              ? Cores.primarioHover
              : Cores.primario,
        ),
        foregroundColor: WidgetStateProperty.resolveWith(
          (e) => e.contains(WidgetState.disabled) ? Cores.textoSecundario : Cores.sobrePrimario,
        ),
      ),
    ),
    outlinedButtonTheme: OutlinedButtonThemeData(
      style: ButtonStyle(
        minimumSize: const WidgetStatePropertyAll(tamanhoBotao),
        shape: WidgetStatePropertyAll(formaCampo),
        textStyle: rotuloBotao,
        foregroundColor: const WidgetStatePropertyAll(Cores.primario),
        side: _anelFoco(const BorderSide(color: Cores.contorno)),
      ),
    ),
    textButtonTheme: TextButtonThemeData(
      style: ButtonStyle(
        minimumSize: const WidgetStatePropertyAll(tamanhoBotao),
        shape: WidgetStatePropertyAll(formaCampo),
        textStyle: rotuloBotao,
        foregroundColor: const WidgetStatePropertyAll(Cores.primario),
        side: _anelFoco(),
      ),
    ),
    iconButtonTheme: IconButtonThemeData(
      style: ButtonStyle(minimumSize: const WidgetStatePropertyAll(Size.square(48)), side: _anelFoco()),
    ),
    cardTheme: CardThemeData(
      color: Cores.superficie,
      surfaceTintColor: Colors.transparent,
      elevation: 0,
      margin: EdgeInsets.zero,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(Raio.cartao),
        side: const BorderSide(color: Cores.divisor),
      ),
    ),
    inputDecorationTheme: InputDecorationThemeData(
      filled: true,
      fillColor: Cores.superficie,
      contentPadding: const EdgeInsets.symmetric(horizontal: Espaco.m, vertical: 14),
      border: borda(Cores.contorno),
      enabledBorder: borda(Cores.contorno),
      focusedBorder: borda(Cores.foco, 2),
      errorBorder: borda(Cores.erro),
      focusedErrorBorder: borda(Cores.erro, 2),
      labelStyle: texto.bodyLarge?.copyWith(color: Cores.textoSecundario),
      floatingLabelStyle: texto.bodyMedium?.copyWith(color: Cores.texto),
      helperStyle: texto.bodySmall,
      errorStyle: texto.bodySmall?.copyWith(color: Cores.erro),
      errorMaxLines: 3,
      helperMaxLines: 3,
    ),
    dialogTheme: DialogThemeData(
      backgroundColor: Cores.superficie,
      surfaceTintColor: Colors.transparent,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(Raio.dialogo)),
      titleTextStyle: texto.titleLarge,
    ),
    chipTheme: ChipThemeData(
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(Raio.campo)),
      side: const BorderSide(color: Cores.contorno),
      labelStyle: texto.labelLarge,
      selectedColor: Cores.sucessoFundo,
      checkmarkColor: Cores.primario,
    ),
    navigationBarTheme: NavigationBarThemeData(
      backgroundColor: Cores.superficie,
      indicatorColor: Cores.sucessoFundo,
      surfaceTintColor: Colors.transparent,
      labelTextStyle: WidgetStatePropertyAll(texto.labelMedium),
      elevation: 1,
    ),
    navigationRailTheme: NavigationRailThemeData(
      backgroundColor: Cores.superficie,
      indicatorColor: Cores.sucessoFundo,
      selectedLabelTextStyle: texto.labelLarge?.copyWith(color: Cores.texto),
      unselectedLabelTextStyle: texto.labelLarge?.copyWith(color: Cores.textoSecundario),
    ),
    navigationDrawerTheme: NavigationDrawerThemeData(
      backgroundColor: Cores.superficie,
      indicatorColor: Cores.sucessoFundo,
      surfaceTintColor: Colors.transparent,
      labelTextStyle: WidgetStatePropertyAll(texto.labelLarge),
    ),
    snackBarTheme: SnackBarThemeData(
      behavior: SnackBarBehavior.floating,
      backgroundColor: Cores.texto,
      contentTextStyle: texto.bodyMedium?.copyWith(color: Cores.sobrePrimario),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(Raio.campo)),
    ),
    dividerTheme: const DividerThemeData(color: Cores.divisor, space: 1, thickness: 1),
    progressIndicatorTheme: const ProgressIndicatorThemeData(color: Cores.primario),
    switchTheme: SwitchThemeData(
      trackOutlineColor: WidgetStateProperty.resolveWith((e) => e.contains(WidgetState.focused) ? Cores.foco : null),
    ),
  );
}
