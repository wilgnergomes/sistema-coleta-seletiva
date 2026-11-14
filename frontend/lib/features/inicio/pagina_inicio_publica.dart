import 'package:material_ui/material_ui.dart';

import '../../app/tema/tokens.dart';
import '../../core/estado/carga.dart';
import '../../ui/compartilhado/espaco_imagem.dart';
import '../../ui/compartilhado/estados.dart';
import '../../ui/compartilhado/marca.dart';
import '../../ui/compartilhado/material_visual.dart';
import '../../ui/compartilhado/movimento.dart';
import '../../ui/compartilhado/superficie.dart';
import '../catalogo/data/modelos.dart';
import '../catalogo/data/repositorio_catalogo.dart';
import '../catalogo/ui/componentes_catalogo.dart';
import 'ilustracao_recolhimento.dart';

/// Destinos que a página inicial oferece; cada um leva a uma rota real.
class NavegacaoInicio {
  const NavegacaoInicio({
    required this.pontos,
    required this.filtros,
    required this.materiais,
    required this.material,
    required this.cadastro,
    required this.solicitar,
    this.demonstracao,
  });

  final ValueChanged<FiltroPontos> pontos;

  /// Abre o catálogo com o painel de filtros aberto (e o nome já digitado).
  final ValueChanged<String?> filtros;
  final VoidCallback materiais;
  final ValueChanged<int> material;
  final VoidCallback cadastro;

  /// Caminho até uma solicitação: escolher um ponto (morador) ou entrar na
  /// demonstração como morador.
  final VoidCallback solicitar;

  /// Nulo quando a compilação não habilita a demonstração.
  final VoidCallback? demonstracao;
}

/// Larguras de composição da página pública (não são os limiares do shell).
enum _Formato { celular, tablet, desktop }

_Formato _formato(double largura) => largura >= 1000
    ? _Formato.desktop
    : largura >= 700
    ? _Formato.tablet
    : _Formato.celular;

const _larguraConteudo = 1200.0;
const _verdeProfundo = Color(0xFF103A2B);

/// Página inicial pública (`/`): apresentação do serviço em seções com
/// composições diferentes entre si.
class PaginaInicioPublica extends StatefulWidget {
  const PaginaInicioPublica({super.key, required this.catalogo, required this.navegar});

  final RepositorioCatalogo catalogo;
  final NavegacaoInicio navegar;

  @override
  State<PaginaInicioPublica> createState() => _PaginaInicioPublicaState();
}

class _PaginaInicioPublicaState extends State<PaginaInicioPublica> {
  final _materiais = Carga<CatalogoMateriais>();
  final _rolagem = ScrollController();

  void _carregar() => _materiais.executar(widget.catalogo.materiaisAtivos, () {
    if (mounted) setState(() {});
  });

  @override
  void initState() {
    super.initState();
    _carregar();
  }

  @override
  void dispose() {
    _rolagem.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scrollbar(
      controller: _rolagem,
      child: SingleChildScrollView(
        controller: _rolagem,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            _Heroi(navegar: widget.navegar),
            const _Percurso(),
            _Materiais(carga: _materiais, navegar: widget.navegar, aoTentarNovamente: _carregar),
            _Recolhimento(navegar: widget.navegar),
            _Encontrar(navegar: widget.navegar, materiais: _materiais.dados),
            _Encerramento(navegar: widget.navegar),
            _Rodape(navegar: widget.navegar),
          ],
        ),
      ),
    );
  }
}

/// Área centralizada com largura máxima e margens por formato.
class _Conteudo extends StatelessWidget {
  const _Conteudo({required this.child, this.vertical = 96});

  final Widget child;
  final double vertical;

  @override
  Widget build(BuildContext context) {
    final f = _formato(MediaQuery.sizeOf(context).width);
    final h = switch (f) {
      _Formato.celular => 20.0,
      _Formato.tablet => 40.0,
      _Formato.desktop => 56.0,
    };
    final v = f == _Formato.celular ? vertical * 0.6 : vertical;
    return Padding(
      padding: EdgeInsets.symmetric(horizontal: h, vertical: v),
      child: Center(
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: _larguraConteudo),
          child: child,
        ),
      ),
    );
  }
}

/// Rótulo curto acima dos títulos de seção.
class _Sobretitulo extends StatelessWidget {
  const _Sobretitulo(this.texto);

  final String texto;

  @override
  Widget build(BuildContext context) => Text(
    texto.toUpperCase(),
    style: Theme.of(context).textTheme.labelMedium
        ?.copyWith(color: Cores.primario, letterSpacing: 1.6, fontWeight: FontWeight.w600),
  );
}

TextStyle? _tituloSecao(BuildContext context) {
  final f = _formato(MediaQuery.sizeOf(context).width);
  final tamanho = switch (f) {
    _Formato.celular => 30.0,
    _Formato.tablet => 38.0,
    _Formato.desktop => 44.0,
  };
  return Theme.of(context).textTheme.displaySmall
      ?.copyWith(fontSize: tamanho, height: 1.12, letterSpacing: -0.6, color: Cores.texto);
}

// ---------------------------------------------------------------------------
// Seção 1 · Apresentação

class _Heroi extends StatelessWidget {
  const _Heroi({required this.navegar});

  final NavegacaoInicio navegar;

  @override
  Widget build(BuildContext context) {
    final largura = MediaQuery.sizeOf(context).width;
    final f = _formato(largura);
    final texto = Theme.of(context).textTheme;
    final tamanhoTitulo = switch (f) {
      _Formato.celular => largura < 380 ? 40.0 : 44.0,
      _Formato.tablet => 52.0,
      _Formato.desktop => 68.0,
    };
    final titulo = f == _Formato.celular ? 'Reciclar ficou\nmais simples.' : 'Reciclar\nficou mais\nsimples.';

    return ClipRect(
      child: Stack(
        children: [
          // Formas discretas de fundo, fora do fluxo de leitura.
          Positioned(
            right: f == _Formato.celular ? -160 : -80,
            top: f == _Formato.celular ? 300 : -120,
            child: const _Circulo(diametro: 520, cor: Color(0xFFE3F2E9)),
          ),
          const Positioned(left: -60, bottom: -80, child: _Circulo(diametro: 220, cor: Color(0xFFF1F7F2))),
          _Conteudo(
            vertical: f == _Formato.desktop ? 88 : 64,
            child: EntradaCoordenada(
              construir: (context, entrada) {
                final textos = Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    entrada(
                      0,
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: Espaco.s, vertical: 6),
                        decoration: BoxDecoration(
                          color: Colors.white,
                          borderRadius: BorderRadius.circular(Raio.pilula),
                          border: Border.all(color: Cores.divisor),
                        ),
                        child: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            for (final v in [
                              VisualMaterial.papel,
                              VisualMaterial.plastico,
                              VisualMaterial.vidro,
                              VisualMaterial.metal,
                            ])
                              Container(
                                width: 8,
                                height: 8,
                                margin: const EdgeInsets.only(right: 4),
                                decoration: BoxDecoration(color: v.cor, shape: BoxShape.circle),
                              ),
                            const SizedBox(width: 6),
                            Flexible(child: Text('Coleta seletiva sem complicação', style: texto.labelMedium)),
                          ],
                        ),
                      ),
                    ),
                    const SizedBox(height: Espaco.l),
                    entrada(
                      1,
                      Semantics(
                        header: true,
                        label: 'Reciclar ficou mais simples.',
                        excludeSemantics: true,
                        child: Text(
                          titulo,
                          style: texto.displaySmall?.copyWith(
                            fontSize: tamanhoTitulo,
                            height: 1.04,
                            letterSpacing: -1.4,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                      ),
                    ),
                    const SizedBox(height: Espaco.l),
                    entrada(
                      2,
                      ConstrainedBox(
                        constraints: const BoxConstraints(maxWidth: 520),
                        child: Text(
                          'Descubra pontos de coleta, saiba como separar seus materiais e solicite o recolhimento sem complicações.',
                          style: texto.titleMedium?.copyWith(
                            fontWeight: FontWeight.w400,
                            color: Cores.textoSecundario,
                            fontSize: f == _Formato.celular ? 17 : 19,
                            height: 1.55,
                          ),
                        ),
                      ),
                    ),
                    const SizedBox(height: Espaco.xl),
                    entrada(
                      3,
                      BotaoDestaque(
                        rotulo: 'Encontrar um ponto de coleta',
                        aoPressionar: () => navegar.pontos(const FiltroPontos()),
                      ),
                    ),
                    const SizedBox(height: Espaco.s),
                    entrada(
                      4,
                      Wrap(
                        spacing: Espaco.s,
                        runSpacing: Espaco.xxs,
                        children: [
                          LinkDiscreto(rotulo: 'Conhecer os materiais', aoPressionar: navegar.materiais),
                          if (navegar.demonstracao != null)
                            LinkDiscreto(rotulo: 'Experimentar a demonstração', aoPressionar: navegar.demonstracao!),
                        ],
                      ),
                    ),
                  ],
                );
                final visual = entrada(2, const _ComposicaoHeroi(), deslocamento: const Offset(32, 0));
                if (f == _Formato.celular) {
                  return Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      textos,
                      const SizedBox(height: Espaco.xxl),
                      Center(
                        child: ConstrainedBox(constraints: const BoxConstraints(maxWidth: 420), child: visual),
                      ),
                    ],
                  );
                }
                return Row(
                  crossAxisAlignment: CrossAxisAlignment.center,
                  children: [
                    Expanded(flex: 55, child: textos),
                    SizedBox(width: f == _Formato.desktop ? 56 : 32),
                    Expanded(flex: 45, child: visual),
                  ],
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}

class _Circulo extends StatelessWidget {
  const _Circulo({required this.diametro, required this.cor, this.borda = false});

  final double diametro;
  final Color cor;
  final bool borda;

  @override
  Widget build(BuildContext context) => ExcludeSemantics(
    child: Container(
      width: diametro,
      height: diametro,
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        color: borda ? null : cor,
        border: borda ? Border.all(color: cor, width: 1.5) : null,
      ),
    ),
  );
}

/// Composição visual do hero: espaço da imagem principal (provisório: o
/// símbolo atual sobre um fundo em degradê) com camadas sobrepostas.
class _ComposicaoHeroi extends StatelessWidget {
  const _ComposicaoHeroi();

  @override
  Widget build(BuildContext context) {
    final texto = Theme.of(context).textTheme;
    Widget etiqueta(IconData icone, String rotulo, Color cor) => Container(
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(Raio.pilula),
        boxShadow: Sombra.elevada,
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icone, size: 18, color: cor),
          const SizedBox(width: 8),
          Text(rotulo, style: texto.labelLarge),
        ],
      ),
    );
    return ExcludeSemantics(
      child: AspectRatio(
        aspectRatio: 1.05,
        child: LayoutBuilder(
          builder: (context, c) => Stack(
            clipBehavior: Clip.none,
            children: [
              Positioned(
                left: c.maxWidth * 0.08,
                top: c.maxHeight * 0.04,
                right: 0,
                bottom: c.maxHeight * 0.1,
                child: Transform.rotate(
                  angle: 0.05,
                  child: Container(
                    decoration: BoxDecoration(color: Cores.primario, borderRadius: BorderRadius.circular(36)),
                  ),
                ),
              ),
              Positioned(
                left: 0,
                top: c.maxHeight * 0.1,
                right: c.maxWidth * 0.06,
                child: Container(
                  decoration: BoxDecoration(borderRadius: BorderRadius.circular(32), boxShadow: Sombra.elevada),
                  child: EspacoImagem(
                    proporcao: 4 / 3,
                    raio: 32,
                    asset: AssetsVisuais.heroi,
                    provisorio: Container(
                      decoration: const BoxDecoration(
                        gradient: LinearGradient(
                          begin: Alignment.topLeft,
                          end: Alignment.bottomRight,
                          colors: [Color(0xFFF7FBF8), Color(0xFFDCEFE3)],
                        ),
                      ),
                      child: FractionallySizedBox(
                        widthFactor: 0.62,
                        heightFactor: 0.62,
                        child: LayoutBuilder(
                          builder: (context, d) => Center(child: SimboloReciclagem(tamanho: d.biggest.shortestSide)),
                        ),
                      ),
                    ),
                  ),
                ),
              ),
              Positioned(
                left: -c.maxWidth * 0.04,
                bottom: c.maxHeight * 0.06,
                child: etiqueta(Icons.local_shipping_outlined, 'Recolhimento em casa', Cores.primario),
              ),
              Positioned(
                right: 0,
                top: 0,
                child: etiqueta(Icons.place_outlined, 'Pontos no seu bairro', VisualMaterial.papel.cor),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// Seção 2 · Como funciona (percurso conectado)

class _Passo {
  const _Passo(this.icone, this.titulo, this.descricao);

  final IconData icone;
  final String titulo;
  final String descricao;
}

const _passos = [
  _Passo(Icons.menu_book_outlined, 'Conheça os materiais', 'Veja o que é reciclável e como preparar cada item.'),
  _Passo(Icons.travel_explore, 'Encontre um ponto', 'Busque pontos que aceitam os seus materiais.'),
  _Passo(Icons.local_shipping_outlined, 'Peça o recolhimento', 'Quando o ponto oferece, ele busca no seu endereço.'),
  _Passo(Icons.track_changes, 'Acompanhe', 'Siga a solicitação pelo protocolo até a coleta.'),
];

class _Percurso extends StatelessWidget {
  const _Percurso();

  @override
  Widget build(BuildContext context) {
    final f = _formato(MediaQuery.sizeOf(context).width);
    return Container(
      color: Colors.white,
      child: _Conteudo(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Revelar(
              efeito: EfeitoEntrada.daEsquerda,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const _Sobretitulo('Como funciona'),
                  const SizedBox(height: Espaco.s),
                  Semantics(
                    header: true,
                    child: Text('Do separar ao recolher,\nem quatro passos.', style: _tituloSecao(context)),
                  ),
                ],
              ),
            ),
            SizedBox(height: f == _Formato.desktop ? 72 : 40),
            if (f == _Formato.desktop) const _PercursoHorizontal() else const _PercursoVertical(),
          ],
        ),
      ),
    );
  }
}

class _NoPercurso extends StatelessWidget {
  const _NoPercurso({required this.indice});

  final int indice;

  @override
  Widget build(BuildContext context) => Container(
    width: 56,
    height: 56,
    decoration: BoxDecoration(
      color: indice.isEven ? Cores.primario : Colors.white,
      shape: BoxShape.circle,
      border: Border.all(color: Cores.primario, width: 2),
      boxShadow: Sombra.repouso,
    ),
    child: Icon(_passos[indice].icone, color: indice.isEven ? Colors.white : Cores.primario),
  );
}

class _TextoPasso extends StatelessWidget {
  const _TextoPasso({required this.indice});

  final int indice;

  @override
  Widget build(BuildContext context) {
    final texto = Theme.of(context).textTheme;
    final p = _passos[indice];
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      mainAxisSize: MainAxisSize.min,
      children: [
        Text(
          '0${indice + 1}',
          style: texto.displaySmall?.copyWith(
            fontSize: 44,
            height: 1,
            color: Cores.secundarioSuave,
            fontWeight: FontWeight.w600,
          ),
        ),
        const SizedBox(height: Espaco.xs),
        Text(p.titulo, style: texto.titleLarge, maxLines: 2, overflow: TextOverflow.ellipsis),
        const SizedBox(height: Espaco.xxs),
        Text(
          p.descricao,
          style: texto.bodyLarge?.copyWith(color: Cores.textoSecundario),
          maxLines: 3,
          overflow: TextOverflow.ellipsis,
        ),
      ],
    );
  }
}

/// Desktop: linha de progresso com os nós e textos alternando acima e abaixo.
class _PercursoHorizontal extends StatelessWidget {
  const _PercursoHorizontal();

  @override
  Widget build(BuildContext context) {
    final escala = (MediaQuery.textScalerOf(context).scale(16) / 16).clamp(1.0, 2.0);
    // Número (44) + título (2 × 28) + descrição (3 × 24) + espaços.
    final alturaTexto = 200 * escala;
    return Stack(
      children: [
        // Linha que conecta as etapas, atravessando os nós.
        Positioned(
          left: 0,
          right: 0,
          top: alturaTexto + Espaco.xs + 27,
          child: Container(
            height: 2,
            decoration: const BoxDecoration(
              gradient: LinearGradient(colors: [Cores.primario, Cores.secundarioSuave, Cores.primario]),
            ),
          ),
        ),
        Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            for (var i = 0; i < _passos.length; i++)
              Expanded(
                child: Revelar(
                  atraso: Duration(milliseconds: 120 * i),
                  efeito: i.isEven ? EfeitoEntrada.subir : EfeitoEntrada.escala,
                  child: Padding(
                    padding: const EdgeInsets.symmetric(horizontal: Espaco.s),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        SizedBox(
                          height: alturaTexto,
                          child: i.isEven
                              ? Align(
                                  alignment: Alignment.bottomLeft,
                                  child: _TextoPasso(indice: i),
                                )
                              : const SizedBox.shrink(),
                        ),
                        const SizedBox(height: Espaco.xs),
                        _NoPercurso(indice: i),
                        const SizedBox(height: Espaco.s),
                        SizedBox(
                          height: alturaTexto,
                          child: i.isOdd
                              ? Align(
                                  alignment: Alignment.topLeft,
                                  child: _TextoPasso(indice: i),
                                )
                              : const SizedBox.shrink(),
                        ),
                      ],
                    ),
                  ),
                ),
              ),
          ],
        ),
      ],
    );
  }
}

/// Celular e tablet: sequência vertical com o fio conectando os nós.
class _PercursoVertical extends StatelessWidget {
  const _PercursoVertical();

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        for (var i = 0; i < _passos.length; i++)
          Revelar(
            atraso: Duration(milliseconds: 80 * i),
            efeito: EfeitoEntrada.daEsquerda,
            child: IntrinsicHeight(
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Column(
                    children: [
                      _NoPercurso(indice: i),
                      if (i < _passos.length - 1) Expanded(child: Container(width: 2, color: Cores.secundarioSuave)),
                    ],
                  ),
                  const SizedBox(width: Espaco.m),
                  Expanded(
                    child: Padding(
                      padding: EdgeInsets.only(bottom: i < _passos.length - 1 ? Espaco.xl : 0),
                      child: _TextoPasso(indice: i),
                    ),
                  ),
                ],
              ),
            ),
          ),
      ],
    );
  }
}

// ---------------------------------------------------------------------------
// Seção 3 · Materiais (composição assimétrica com os quatro tradicionais)

class _Materiais extends StatelessWidget {
  const _Materiais({required this.carga, required this.navegar, required this.aoTentarNovamente});

  final Carga<CatalogoMateriais> carga;
  final NavegacaoInicio navegar;
  final VoidCallback aoTentarNovamente;

  @override
  Widget build(BuildContext context) {
    final f = _formato(MediaQuery.sizeOf(context).width);
    return _Conteudo(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Revelar(
            child: Wrap(
              alignment: WrapAlignment.spaceBetween,
              crossAxisAlignment: WrapCrossAlignment.end,
              runSpacing: Espaco.m,
              spacing: Espaco.l,
              children: [
                Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    const _Sobretitulo('Materiais'),
                    const SizedBox(height: Espaco.s),
                    Semantics(
                      header: true,
                      child: Text('Cada material\ntem o seu caminho.', style: _tituloSecao(context)),
                    ),
                  ],
                ),
                LinkDiscreto(rotulo: 'Ver todos os materiais', aoPressionar: navegar.materiais),
              ],
            ),
          ),
          SizedBox(height: f == _Formato.celular ? Espaco.l : 48),
          ConteudoCarga<CatalogoMateriais>(
            carga: carga,
            rotuloCarregando: 'Carregando materiais…',
            aoTentarNovamente: aoTentarNovamente,
            construir: (context, catalogo) => _ComposicaoMateriais(catalogo: catalogo, navegar: navegar),
          ),
        ],
      ),
    );
  }
}

class _ComposicaoMateriais extends StatelessWidget {
  const _ComposicaoMateriais({required this.catalogo, required this.navegar});

  final CatalogoMateriais catalogo;
  final NavegacaoInicio navegar;

  @override
  Widget build(BuildContext context) {
    if (catalogo.itens.isEmpty) {
      return const EstadoVazio(icone: Icons.recycling, titulo: 'Nenhum material ativo no catálogo');
    }
    MaterialColeta? porNome(String nome) =>
        catalogo.itens.where((m) => normalizarNome(m.nome) == normalizarNome(nome)).firstOrNull;
    final tradicionais = [for (final n in materiaisTradicionais) porNome(n)].whereType<MaterialColeta>().toList();
    final outros = catalogo.itens.where((m) => !tradicionais.contains(m)).toList();
    final f = _formato(MediaQuery.sizeOf(context).width);
    final escala = (MediaQuery.textScalerOf(context).scale(16) / 16).clamp(1.0, 2.0);

    Widget tile(MaterialColeta m, {required bool grande, required double altura, int ordem = 0}) => Revelar(
      atraso: Duration(milliseconds: 90 * ordem),
      efeito: grande ? EfeitoEntrada.escala : EfeitoEntrada.subir,
      child: _TileMaterial(material: m, grande: grande, altura: altura * escala, aoAbrir: () => navegar.material(m.id)),
    );

    Widget composicao = const SizedBox.shrink();
    if (tradicionais.isNotEmpty && f == _Formato.desktop) {
      final resto = tradicionais.skip(1).toList();
      composicao = Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Expanded(flex: 5, child: tile(tradicionais.first, grande: true, altura: 432)),
          const SizedBox(width: Espaco.m),
          Expanded(
            flex: 7,
            child: Column(
              children: [
                Row(
                  children: [
                    for (var i = 0; i < resto.length && i < 2; i++) ...[
                      if (i > 0) const SizedBox(width: Espaco.m),
                      Expanded(child: tile(resto[i], grande: false, altura: 208, ordem: i + 1)),
                    ],
                  ],
                ),
                if (resto.length > 2) ...[
                  const SizedBox(height: Espaco.m),
                  tile(resto[2], grande: false, altura: 208, ordem: 3),
                ],
              ],
            ),
          ),
        ],
      );
    } else if (tradicionais.isNotEmpty) {
      final colunas = f == _Formato.tablet ? 2 : 1;
      final resto = tradicionais.skip(1).toList();
      composicao = Column(
        children: [
          tile(tradicionais.first, grande: true, altura: f == _Formato.tablet ? 300 : 280),
          const SizedBox(height: Espaco.m),
          for (var i = 0; i < resto.length; i += colunas) ...[
            Row(
              children: [
                for (var j = i; j < i + colunas; j++) ...[
                  if (j > i) const SizedBox(width: Espaco.m),
                  Expanded(
                    child: j < resto.length
                        ? tile(resto[j], grande: false, altura: 196, ordem: j + 1)
                        : const SizedBox.shrink(),
                  ),
                ],
              ],
            ),
            const SizedBox(height: Espaco.m),
          ],
        ],
      );
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        composicao,
        if (outros.isNotEmpty) ...[
          const SizedBox(height: Espaco.l),
          Revelar(
            child: Wrap(
              spacing: Espaco.xs,
              runSpacing: Espaco.xs,
              crossAxisAlignment: WrapCrossAlignment.center,
              children: [
                Padding(
                  padding: const EdgeInsets.only(right: Espaco.xs),
                  child: Text('Também aceitos:', style: Theme.of(context).textTheme.titleSmall),
                ),
                for (final m in outros)
                  ActionChip(
                    avatar: IconeMaterial(nome: m.nome, tamanho: 22),
                    label: Text(m.nome),
                    onPressed: () => navegar.material(m.id),
                    tooltip: 'Ver orientações de ${m.nome}',
                  ),
              ],
            ),
          ),
        ],
      ],
    );
  }
}

/// Bloco de material com fundo na cor do material (tom suave), ícone em
/// destaque e acesso à orientação completa.
class _TileMaterial extends StatefulWidget {
  const _TileMaterial({required this.material, required this.grande, required this.altura, required this.aoAbrir});

  final MaterialColeta material;
  final bool grande;
  final double altura;
  final VoidCallback aoAbrir;

  @override
  State<_TileMaterial> createState() => _TileMaterialState();
}

class _TileMaterialState extends State<_TileMaterial> {
  bool _ativo = false;

  @override
  Widget build(BuildContext context) {
    final m = widget.material;
    final visual = VisualMaterial.porNome(m.nome);
    final texto = Theme.of(context).textTheme;
    final fundo = Color.alphaBlend(visual.cor.withValues(alpha: 0.10), Colors.white);
    return Semantics(
      button: true,
      label: 'Ver orientações de ${m.nome}',
      excludeSemantics: true,
      child: AnimatedContainer(
        duration: duracao(context, Movimento.hover.inMilliseconds),
        curve: Curves.easeOut,
        height: widget.altura,
        transform: Matrix4.translationValues(0, _ativo ? -4 : 0, 0),
        decoration: BoxDecoration(
          color: fundo,
          borderRadius: BorderRadius.circular(28),
          border: Border.all(color: _ativo ? visual.cor : Colors.transparent, width: 2),
          boxShadow: _ativo ? Sombra.elevada : const [],
        ),
        child: Material(
          type: MaterialType.transparency,
          child: InkWell(
            borderRadius: BorderRadius.circular(28),
            onTap: widget.aoAbrir,
            onHover: (v) => setState(() => _ativo = v),
            onFocusChange: (v) => setState(() => _ativo = v),
            child: Stack(
              children: [
                // Forma decorativa no canto, na cor do material.
                Positioned(
                  right: -40,
                  bottom: -40,
                  child: ExcludeSemantics(
                    child: Container(
                      width: widget.grande ? 260 : 150,
                      height: widget.grande ? 260 : 150,
                      decoration: BoxDecoration(shape: BoxShape.circle, color: visual.cor.withValues(alpha: 0.10)),
                    ),
                  ),
                ),
                Padding(
                  padding: EdgeInsets.all(widget.grande ? 32 : 24),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      IconeMaterial(nome: m.nome, tamanho: widget.grande ? 72 : 48),
                      const Spacer(),
                      Text(
                        m.nome,
                        style: (widget.grande ? texto.headlineMedium : texto.titleLarge)?.copyWith(letterSpacing: -0.3),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                      const SizedBox(height: Espaco.xxs),
                      Text(
                        m.descricao,
                        style: texto.bodyLarge?.copyWith(color: Cores.textoSecundario),
                        maxLines: widget.grande ? 3 : 2,
                        overflow: TextOverflow.ellipsis,
                      ),
                      const SizedBox(height: Espaco.s),
                      Row(
                        children: [
                          Flexible(
                            child: Text(
                              'Como preparar',
                              style: texto.labelLarge,
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                            ),
                          ),
                          AnimatedSlide(
                            offset: Offset(_ativo ? 0.3 : 0, 0),
                            duration: duracao(context, Movimento.hover.inMilliseconds),
                            child: const Icon(Icons.arrow_forward, size: 18),
                          ),
                        ],
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// Seção 4 · Recolhimento domiciliar

class _Recolhimento extends StatelessWidget {
  const _Recolhimento({required this.navegar});

  final NavegacaoInicio navegar;

  static const _jornada = [
    'Escolha um ponto que ofereça recolhimento domiciliar.',
    'Informe os materiais, as quantidades e o endereço.',
    'O operador do ponto combina a data da coleta.',
    'Acompanhe tudo pelo protocolo da solicitação.',
  ];

  @override
  Widget build(BuildContext context) {
    final f = _formato(MediaQuery.sizeOf(context).width);
    final texto = Theme.of(context).textTheme;
    final visual = Revelar(
      efeito: EfeitoEntrada.daEsquerda,
      deslocamento: 32,
      child: Padding(
        padding: const EdgeInsets.only(right: 20, bottom: 20),
        child: Stack(
          clipBehavior: Clip.none,
          children: [
            // Camada de profundidade deslocada atrás da imagem.
            Positioned(
              left: 20,
              top: 20,
              right: -20,
              bottom: -20,
              child: Container(
                decoration: BoxDecoration(
                  color: Cores.secundarioSuave.withValues(alpha: 0.35),
                  borderRadius: BorderRadius.circular(32),
                ),
              ),
            ),
            const EspacoImagem(
              proporcao: 5 / 4,
              raio: 32,
              asset: AssetsVisuais.recolhimento,
              provisorio: IlustracaoRecolhimento(altura: double.infinity),
            ),
          ],
        ),
      ),
    );
    final conteudo = Revelar(
      efeito: EfeitoEntrada.daDireita,
      deslocamento: 32,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const _Sobretitulo('Recolhimento domiciliar'),
          const SizedBox(height: Espaco.s),
          Semantics(header: true, child: Text('O ponto vai\naté a sua casa.', style: _tituloSecao(context))),
          const SizedBox(height: Espaco.m),
          Text(
            'Alguns pontos buscam os recicláveis no seu endereço. Você pede e acompanha; o ponto avalia e combina a data.',
            style: texto.bodyLarge?.copyWith(color: Cores.textoSecundario, height: 1.55),
          ),
          const SizedBox(height: Espaco.l),
          for (var i = 0; i < _jornada.length; i++)
            Padding(
              padding: const EdgeInsets.only(bottom: Espaco.s),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Container(
                    width: 28,
                    height: 28,
                    alignment: Alignment.center,
                    decoration: const BoxDecoration(color: Cores.primario, shape: BoxShape.circle),
                    child: Text('${i + 1}', style: texto.labelMedium?.copyWith(color: Colors.white)),
                  ),
                  const SizedBox(width: Espaco.s),
                  Expanded(
                    child: Padding(
                      padding: const EdgeInsets.only(top: 3),
                      child: Text(_jornada[i], style: texto.bodyLarge),
                    ),
                  ),
                ],
              ),
            ),
          const SizedBox(height: Espaco.xs),
          Text('Não há agendamento automático: a data depende da avaliação do ponto.', style: texto.bodySmall),
          const SizedBox(height: Espaco.l),
          BotaoDestaque(
            rotulo: 'Solicitar recolhimento',
            icone: Icons.local_shipping_outlined,
            aoPressionar: navegar.solicitar,
          ),
        ],
      ),
    );
    return Container(
      decoration: const BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
          colors: [Color(0xFFE6F3EB), Color(0xFFF2F8F4)],
        ),
      ),
      child: _Conteudo(
        child: f == _Formato.celular
            ? Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [visual, const SizedBox(height: 48), conteudo],
              )
            : Row(
                crossAxisAlignment: CrossAxisAlignment.center,
                children: [
                  Expanded(flex: 6, child: visual),
                  SizedBox(width: f == _Formato.desktop ? 72 : 40),
                  Expanded(flex: 5, child: conteudo),
                ],
              ),
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// Seção 5 · Encontrar pontos

class _Encontrar extends StatefulWidget {
  const _Encontrar({required this.navegar, required this.materiais});

  final NavegacaoInicio navegar;
  final CatalogoMateriais? materiais;

  @override
  State<_Encontrar> createState() => _EncontrarState();
}

class _EncontrarState extends State<_Encontrar> {
  final _nome = TextEditingController();

  @override
  void dispose() {
    _nome.dispose();
    super.dispose();
  }

  String? get _texto => _nome.text.trim().isEmpty ? null : _nome.text.trim();

  @override
  Widget build(BuildContext context) {
    final texto = Theme.of(context).textTheme;
    final f = _formato(MediaQuery.sizeOf(context).width);
    return Container(
      color: Colors.white,
      child: _Conteudo(
        child: Revelar(
          efeito: EfeitoEntrada.escala,
          child: Column(
            children: [
              const _Sobretitulo('Encontre um ponto'),
              const SizedBox(height: Espaco.s),
              Semantics(
                header: true,
                child: Text(
                  'Onde levar os seus recicláveis?',
                  style: _tituloSecao(context),
                  textAlign: TextAlign.center,
                ),
              ),
              const SizedBox(height: Espaco.s),
              Text(
                'Pesquise pelo nome do ponto ou filtre por município, bairro e material aceito.',
                style: texto.bodyLarge?.copyWith(color: Cores.textoSecundario),
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: Espaco.xl),
              ConstrainedBox(
                constraints: const BoxConstraints(maxWidth: 760),
                child: Container(
                  padding: const EdgeInsets.all(8),
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(Raio.pilula),
                    border: Border.all(color: Cores.divisor),
                    boxShadow: Sombra.elevada,
                  ),
                  child: Row(
                    children: [
                      const SizedBox(width: Espaco.s),
                      const ExcludeSemantics(child: Icon(Icons.search, color: Cores.textoSecundario)),
                      const SizedBox(width: Espaco.xs),
                      Expanded(
                        child: TextField(
                          controller: _nome,
                          textInputAction: TextInputAction.search,
                          onSubmitted: (_) => widget.navegar.pontos(FiltroPontos(nome: _texto)),
                          decoration: const InputDecoration(
                            hintText: 'Nome do ponto de coleta',
                            labelText: 'Nome do ponto de coleta',
                            floatingLabelBehavior: FloatingLabelBehavior.never,
                            border: InputBorder.none,
                            enabledBorder: InputBorder.none,
                            focusedBorder: InputBorder.none,
                            filled: false,
                          ),
                        ),
                      ),
                      if (f != _Formato.celular) ...[
                        TextButton.icon(
                          onPressed: () => widget.navegar.filtros(_texto),
                          icon: const Icon(Icons.tune),
                          label: const Text('Filtros'),
                        ),
                        const SizedBox(width: Espaco.xxs),
                        FilledButton(
                          style: FilledButton.styleFrom(shape: const StadiumBorder()),
                          onPressed: () => widget.navegar.pontos(FiltroPontos(nome: _texto)),
                          child: const Text('Buscar'),
                        ),
                      ] else
                        IconButton.filled(
                          tooltip: 'Buscar',
                          onPressed: () => widget.navegar.pontos(FiltroPontos(nome: _texto)),
                          icon: const Icon(Icons.arrow_forward),
                        ),
                    ],
                  ),
                ),
              ),
              if (f == _Formato.celular) ...[
                const SizedBox(height: Espaco.s),
                OutlinedButton.icon(
                  onPressed: () => widget.navegar.filtros(_texto),
                  icon: const Icon(Icons.tune),
                  label: const Text('Filtrar por município, bairro ou material'),
                ),
              ],
              const SizedBox(height: Espaco.xl),
              Text('Ou comece pelo material', style: texto.titleSmall),
              const SizedBox(height: Espaco.xs),
              Center(
                child: AtalhosMateriais(
                  catalogo: widget.materiais,
                  selecionado: null,
                  aoSelecionar: (m) => widget.navegar.pontos(FiltroPontos(materialId: m?.id)),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// Seção 6 · Encerramento e rodapé

class _Encerramento extends StatelessWidget {
  const _Encerramento({required this.navegar});

  final NavegacaoInicio navegar;

  @override
  Widget build(BuildContext context) {
    final f = _formato(MediaQuery.sizeOf(context).width);
    final texto = Theme.of(context).textTheme;
    final tamanho = switch (f) {
      _Formato.celular => 40.0,
      _Formato.tablet => 56.0,
      _Formato.desktop => 72.0,
    };
    final frase = Semantics(
      header: true,
      label: 'Separe hoje. O resto fica mais simples.',
      excludeSemantics: true,
      child: Text.rich(
        const TextSpan(
          children: [
            TextSpan(text: 'Separe hoje.\n'),
            TextSpan(
              text: 'O resto fica\nmais simples.',
              style: TextStyle(color: Cores.secundarioSuave),
            ),
          ],
        ),
        style: texto.displaySmall?.copyWith(
          fontSize: tamanho,
          height: 1.02,
          letterSpacing: -1.6,
          color: Colors.white,
          fontWeight: FontWeight.w600,
        ),
      ),
    );
    final acoes = Column(
      crossAxisAlignment: f == _Formato.celular ? CrossAxisAlignment.start : CrossAxisAlignment.end,
      mainAxisSize: MainAxisSize.min,
      children: [
        Text(
          'Encontre o ponto certo para cada material.',
          style: texto.titleMedium?.copyWith(color: Colors.white, fontWeight: FontWeight.w400),
        ),
        const SizedBox(height: Espaco.m),
        BotaoDestaque(
          rotulo: 'Encontrar um ponto',
          claro: true,
          aoPressionar: () => navegar.pontos(const FiltroPontos()),
        ),
        if (navegar.demonstracao != null) ...[
          const SizedBox(height: Espaco.xs),
          LinkDiscreto(rotulo: 'Experimentar a demonstração', aoPressionar: navegar.demonstracao!, claro: true),
        ],
      ],
    );
    return ClipRect(
      child: Container(
        color: _verdeProfundo,
        child: Stack(
          children: [
            Positioned(
              right: -140,
              top: -140,
              child: _Circulo(diametro: 480, cor: Colors.white.withValues(alpha: 0.10), borda: true),
            ),
            Positioned(
              right: -40,
              top: -40,
              child: _Circulo(diametro: 280, cor: Colors.white.withValues(alpha: 0.08), borda: true),
            ),
            _Conteudo(
              vertical: 112,
              child: Revelar(
                child: f == _Formato.celular
                    ? Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [frase, const SizedBox(height: 40), acoes],
                      )
                    : Row(
                        crossAxisAlignment: CrossAxisAlignment.end,
                        children: [
                          Expanded(child: frase),
                          const SizedBox(width: Espaco.xl),
                          acoes,
                        ],
                      ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _Rodape extends StatelessWidget {
  const _Rodape({required this.navegar});

  final NavegacaoInicio navegar;

  @override
  Widget build(BuildContext context) {
    final texto = Theme.of(context).textTheme;
    return Container(
      color: Colors.white,
      child: _Conteudo(
        vertical: 32,
        child: Wrap(
          alignment: WrapAlignment.spaceBetween,
          crossAxisAlignment: WrapCrossAlignment.center,
          spacing: Espaco.l,
          runSpacing: Espaco.s,
          children: [
            Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: [
                const Marca(tamanho: 28),
                const SizedBox(height: Espaco.xxs),
                Text('Coleta seletiva, do separar ao recolher.', style: texto.bodySmall),
              ],
            ),
            Wrap(
              spacing: Espaco.xxs,
              children: [
                TextButton(onPressed: () => navegar.pontos(const FiltroPontos()), child: const Text('Pontos')),
                TextButton(onPressed: navegar.materiais, child: const Text('Materiais')),
                TextButton(onPressed: navegar.cadastro, child: const Text('Cadastrar-se')),
                if (navegar.demonstracao != null)
                  TextButton(onPressed: navegar.demonstracao, child: const Text('Demonstração')),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
