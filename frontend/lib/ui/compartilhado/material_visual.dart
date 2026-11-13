import 'package:material_ui/material_ui.dart';

import '../../app/tema/tokens.dart';

/// Identidade visual dos materiais pelo código de cores da Resolução CONAMA
/// 275/2001: papel azul, plástico vermelho, vidro verde, metal amarelo e
/// resíduos perigosos laranja. A cor sempre vem acompanhada do nome.
class VisualMaterial {
  const VisualMaterial({required this.cor, required this.sobreCor, required this.icone});

  final Color cor;

  /// Cor do ícone sobre [cor], com contraste mínimo de 3:1.
  final Color sobreCor;
  final IconData icone;

  static const papel = VisualMaterial(
    cor: Color(0xFF1565C0),
    sobreCor: Colors.white,
    icone: Icons.description_outlined,
  );
  static const plastico = VisualMaterial(
    cor: Color(0xFFC62828),
    sobreCor: Colors.white,
    icone: Icons.local_drink_outlined,
  );
  static const vidro = VisualMaterial(cor: Color(0xFF2E7D32), sobreCor: Colors.white, icone: Icons.wine_bar_outlined);
  static const metal = VisualMaterial(cor: Color(0xFFF2C230), sobreCor: Cores.texto, icone: Icons.hardware_outlined);
  static const perigoso = VisualMaterial(
    cor: Color(0xFFE65100),
    sobreCor: Colors.white,
    icone: Icons.battery_std_outlined,
  );
  static const outro = VisualMaterial(cor: Cores.textoSecundario, sobreCor: Colors.white, icone: Icons.recycling);

  /// Escolhe o visual pelo nome cadastrado, sem diferenciar acentos e caixa.
  static VisualMaterial porNome(String nome) {
    final chave = normalizarNome(nome);
    if (chave.startsWith('papel')) return papel;
    if (chave.startsWith('plastico')) return plastico;
    if (chave.startsWith('vidro')) return vidro;
    if (chave.startsWith('metal')) return metal;
    if (chave.startsWith('pilha') || chave.startsWith('bateria')) return perigoso;
    return outro;
  }
}

/// Os quatro materiais tradicionais da coleta seletiva, na ordem dos atalhos.
const materiaisTradicionais = ['Papel', 'Plástico', 'Vidro', 'Metal'];

String normalizarNome(String texto) {
  const mapa = {'á': 'a', 'à': 'a', 'â': 'a', 'ã': 'a', 'é': 'e', 'ê': 'e', 'í': 'i', 'ó': 'o', 'ô': 'o', 'õ': 'o'};
  final minusculo = texto.trim().toLowerCase();
  return [for (final c in minusculo.split('')) mapa[c] ?? (c == 'ú' ? 'u' : (c == 'ç' ? 'c' : c))].join();
}

/// Ícone circular do material. Decorativo: o nome acompanha o ícone.
class IconeMaterial extends StatelessWidget {
  const IconeMaterial({super.key, required this.nome, this.tamanho = 40});

  final String nome;
  final double tamanho;

  @override
  Widget build(BuildContext context) {
    final visual = VisualMaterial.porNome(nome);
    return ExcludeSemantics(
      child: Container(
        width: tamanho,
        height: tamanho,
        decoration: BoxDecoration(color: visual.cor, shape: BoxShape.circle),
        child: Icon(visual.icone, color: visual.sobreCor, size: tamanho * 0.55),
      ),
    );
  }
}

/// Marcador pequeno com nome do material.
class MarcadorMaterial extends StatelessWidget {
  const MarcadorMaterial({super.key, required this.nome});

  final String nome;

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        IconeMaterial(nome: nome, tamanho: 20),
        const SizedBox(width: Espaco.xs),
        Flexible(child: Text(nome, style: Theme.of(context).textTheme.bodyMedium)),
      ],
    );
  }
}
