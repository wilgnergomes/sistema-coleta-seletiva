import 'package:material_ui/material_ui.dart';

/// Assets visuais definitivos, a serem escolhidos pelo estudante.
///
/// Enquanto um caminho for nulo, a interface usa o conteúdo provisório.
/// Para trocar, adicione o arquivo em `assets/imagens/`, declare a pasta no
/// `pubspec.yaml` e preencha o caminho aqui; nenhuma tela precisa mudar.
abstract final class AssetsVisuais {
  /// Logomarca (recomendado: SVG convertido em PNG 512 × 512, fundo transparente).
  static const String? logomarca = null;

  /// Imagem principal da página inicial (proporção 4:3, mínimo 1200 × 900).
  static const String? heroi = null;

  /// Imagem da seção de recolhimento domiciliar (proporção 5:4).
  static const String? recolhimento = null;

  /// Ilustrações das quatro etapas de "Como funciona" (proporção 1:1).
  static const List<String?> etapas = [null, null, null, null];
}

/// Espaço reservado para uma imagem com proporção definida. Mostra o asset
/// quando houver e, até lá, o [provisorio]. Decorativo para leitores de tela.
class EspacoImagem extends StatelessWidget {
  const EspacoImagem({
    super.key,
    required this.proporcao,
    required this.provisorio,
    this.asset,
    this.raio = 28,
    this.ajuste = BoxFit.cover,
  });

  final double proporcao;
  final String? asset;
  final Widget provisorio;
  final double raio;
  final BoxFit ajuste;

  @override
  Widget build(BuildContext context) {
    final caminho = asset;
    return ExcludeSemantics(
      child: AspectRatio(
        aspectRatio: proporcao,
        child: ClipRRect(
          borderRadius: BorderRadius.circular(raio),
          child: caminho == null
              ? provisorio
              : Image.asset(caminho, fit: ajuste, errorBuilder: (_, _, _) => provisorio),
        ),
      ),
    );
  }
}
