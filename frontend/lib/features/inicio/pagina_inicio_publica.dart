import 'package:material_ui/material_ui.dart';

import '../../app/tema/tokens.dart';
import '../../core/estado/carga.dart';
import '../../ui/compartilhado/estados.dart';
import '../../ui/compartilhado/layout.dart' show GradeResponsiva;
import '../../ui/compartilhado/marca.dart';
import '../../ui/compartilhado/material_visual.dart';
import '../../ui/compartilhado/superficie.dart';
import '../catalogo/data/modelos.dart';
import '../catalogo/data/repositorio_catalogo.dart';
import '../catalogo/ui/componentes_catalogo.dart';
import 'ilustracao_recolhimento.dart';

/// Destinos que a página inicial oferece; cada um leva a uma rota real.
class NavegacaoInicio {
  const NavegacaoInicio({
    required this.pontos,
    required this.materiais,
    required this.material,
    required this.cadastro,
    required this.solicitar,
    this.demonstracao,
  });

  final ValueChanged<FiltroPontos> pontos;
  final VoidCallback materiais;
  final ValueChanged<int> material;
  final VoidCallback cadastro;

  /// Caminho até uma solicitação: escolher um ponto (morador) ou entrar na
  /// demonstração como morador.
  final VoidCallback solicitar;

  /// Nulo quando a compilação não habilita a demonstração.
  final VoidCallback? demonstracao;
}

/// Página inicial pública (`/`): apresentação do serviço com seções que
/// surgem conforme a rolagem.
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
        child: SelectionArea(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              _Apresentacao(navegar: widget.navegar),
              _Secao(
                titulo: 'Como funciona',
                subtitulo: 'Quatro passos entre a separação em casa e o material reciclado.',
                child: const _ComoFunciona(),
              ),
              _Secao(
                fundo: Cores.superficie,
                titulo: 'Materiais recicláveis',
                subtitulo: 'Cada material tem seu jeito de preparar. As orientações vêm do catálogo do sistema.',
                rodape: TextButton.icon(
                  onPressed: widget.navegar.materiais,
                  icon: const Icon(Icons.arrow_forward),
                  iconAlignment: IconAlignment.end,
                  label: const Text('Ver todos os materiais'),
                ),
                child: _Materiais(carga: _materiais, aoAbrir: widget.navegar.material, aoTentarNovamente: _carregar),
              ),
              _Secao(child: _Recolhimento(navegar: widget.navegar)),
              _Secao(
                fundo: Cores.superficie,
                titulo: 'Encontre um ponto',
                subtitulo: 'Pesquise pelo nome do ponto ou escolha um material. Município e bairro ficam nos filtros.',
                child: _Encontrar(navegar: widget.navegar, materiais: _materiais.dados),
              ),
              _Encerramento(navegar: widget.navegar),
              _Rodape(navegar: widget.navegar),
            ],
          ),
        ),
      ),
    );
  }
}

/// Seção de largura total, com conteúdo centralizado e título que surge ao
/// entrar na tela.
class _Secao extends StatelessWidget {
  const _Secao({required this.child, this.titulo, this.subtitulo, this.fundo, this.rodape});

  final Widget child;
  final String? titulo;
  final String? subtitulo;
  final Color? fundo;
  final Widget? rodape;

  @override
  Widget build(BuildContext context) {
    final texto = Theme.of(context).textTheme;
    return LayoutBuilder(
      builder: (context, restricoes) {
        final margem = restricoes.maxWidth < Limiar.intermediario ? Espaco.m : Espaco.xl;
        return Container(
          color: fundo,
          padding: EdgeInsets.symmetric(
            horizontal: margem,
            vertical: restricoes.maxWidth < Limiar.intermediario ? 40 : 72,
          ),
          child: Center(
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: Largura.catalogo),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  if (titulo != null)
                    Revelar(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Semantics(header: true, child: Text(titulo!, style: texto.headlineMedium)),
                          if (subtitulo != null) ...[
                            const SizedBox(height: Espaco.xs),
                            ConstrainedBox(
                              constraints: const BoxConstraints(maxWidth: 640),
                              child: Text(subtitulo!, style: texto.bodyLarge?.copyWith(color: Cores.textoSecundario)),
                            ),
                          ],
                          const SizedBox(height: Espaco.l),
                        ],
                      ),
                    ),
                  child,
                  if (rodape != null) ...[
                    const SizedBox(height: Espaco.m),
                    Align(alignment: Alignment.centerLeft, child: rodape),
                  ],
                ],
              ),
            ),
          ),
        );
      },
    );
  }
}

class _Apresentacao extends StatelessWidget {
  const _Apresentacao({required this.navegar});

  final NavegacaoInicio navegar;

  @override
  Widget build(BuildContext context) {
    final texto = Theme.of(context).textTheme;
    return LayoutBuilder(
      builder: (context, restricoes) {
        final amplo = restricoes.maxWidth >= 840;
        final margem = restricoes.maxWidth < Limiar.intermediario ? Espaco.m : Espaco.xl;
        final textos = Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Container(
              padding: const EdgeInsets.symmetric(horizontal: Espaco.s, vertical: Espaco.xxs),
              decoration: BoxDecoration(color: Cores.sucessoFundo, borderRadius: BorderRadius.circular(Raio.pilula)),
              child: Text('Coleta seletiva no seu município', style: texto.labelMedium?.copyWith(color: Cores.sucesso)),
            ),
            const SizedBox(height: Espaco.m),
            Semantics(
              header: true,
              child: Text(
                'Reciclar ficou mais simples.',
                style: (amplo ? texto.displaySmall?.copyWith(fontSize: 48, height: 56 / 48) : texto.displaySmall),
              ),
            ),
            const SizedBox(height: Espaco.m),
            Text(
              'Descubra pontos de coleta, saiba como separar seus materiais e solicite o recolhimento sem complicações.',
              style: texto.titleMedium?.copyWith(
                fontWeight: FontWeight.w400,
                color: Cores.textoSecundario,
                height: 1.5,
              ),
            ),
            const SizedBox(height: Espaco.l),
            Wrap(
              spacing: Espaco.s,
              runSpacing: Espaco.s,
              children: [
                FilledButton.icon(
                  onPressed: () => navegar.pontos(const FiltroPontos()),
                  icon: const Icon(Icons.place_outlined),
                  label: const Text('Encontrar pontos de coleta'),
                ),
                OutlinedButton.icon(
                  onPressed: navegar.materiais,
                  icon: const Icon(Icons.recycling),
                  label: const Text('Conhecer os materiais'),
                ),
                if (navegar.demonstracao != null)
                  OutlinedButton.icon(
                    onPressed: navegar.demonstracao,
                    icon: const Icon(Icons.play_circle_outline),
                    label: const Text('Experimentar demonstração'),
                  ),
                TextButton(onPressed: navegar.cadastro, child: const Text('Cadastrar-se')),
              ],
            ),
            if (navegar.demonstracao != null) ...[
              const SizedBox(height: Espaco.s),
              Text(
                'A demonstração usa identidade simulada e dados fictícios. Não há login real.',
                style: texto.bodySmall,
              ),
            ],
          ],
        );
        final simbolo = TweenAnimationBuilder<double>(
          tween: Tween(begin: 0, end: 1),
          duration: duracao(context, 900),
          curve: Curves.easeOutBack,
          builder: (context, v, _) => Transform.scale(
            scale: 0.85 + 0.15 * v,
            child: Opacity(
              opacity: v.clamp(0.0, 1.0),
              child: SimboloReciclagem(tamanho: amplo ? 300 : 160, rotacao: (1 - v) * -0.08),
            ),
          ),
        );
        return Container(
          decoration: const BoxDecoration(
            gradient: LinearGradient(
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
              colors: [Color(0xFFE3F2E9), Cores.fundo],
            ),
          ),
          padding: EdgeInsets.fromLTRB(margem, amplo ? 80 : 40, margem, amplo ? 88 : 48),
          child: Center(
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: Largura.catalogo),
              child: amplo
                  ? Row(
                      children: [
                        Expanded(flex: 6, child: textos),
                        const SizedBox(width: Espaco.xl),
                        Expanded(
                          flex: 4,
                          child: Center(child: ExcludeSemantics(child: simbolo)),
                        ),
                      ],
                    )
                  : Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        Center(child: ExcludeSemantics(child: simbolo)),
                        const SizedBox(height: Espaco.l),
                        textos,
                      ],
                    ),
            ),
          ),
        );
      },
    );
  }
}

class _ComoFunciona extends StatelessWidget {
  const _ComoFunciona();

  static const _passos = [
    (Icons.menu_book_outlined, 'Conheça os materiais', 'Veja o que é reciclável e como preparar cada item.'),
    (Icons.travel_explore, 'Encontre um ponto', 'Busque pontos que aceitam os seus materiais.'),
    (Icons.local_shipping_outlined, 'Solicite o recolhimento', 'Quando o ponto oferece, ele busca no seu endereço.'),
    (Icons.track_changes, 'Acompanhe', 'Siga a solicitação pelo protocolo até a coleta.'),
  ];

  @override
  Widget build(BuildContext context) {
    final texto = Theme.of(context).textTheme;
    return GradeResponsiva(
      larguraMinima: 230,
      filhos: [
        for (var i = 0; i < _passos.length; i++)
          Revelar(
            atraso: Duration(milliseconds: 90 * i),
            child: CartaoInterativo(
              altura: 196,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Container(
                        width: 48,
                        height: 48,
                        decoration: BoxDecoration(color: Cores.sucessoFundo, borderRadius: BorderRadius.circular(14)),
                        child: Icon(_passos[i].$1, color: Cores.primario),
                      ),
                      const Spacer(),
                      Text('${i + 1}', style: texto.displaySmall?.copyWith(color: Cores.divisor)),
                    ],
                  ),
                  const SizedBox(height: Espaco.m),
                  Text(_passos[i].$2, style: texto.titleMedium, maxLines: 2, overflow: TextOverflow.ellipsis),
                  const SizedBox(height: Espaco.xxs),
                  Text(_passos[i].$3, style: texto.bodyMedium, maxLines: 3, overflow: TextOverflow.ellipsis),
                ],
              ),
            ),
          ),
      ],
    );
  }
}

class _Materiais extends StatelessWidget {
  const _Materiais({required this.carga, required this.aoAbrir, required this.aoTentarNovamente});

  final Carga<CatalogoMateriais> carga;
  final ValueChanged<int> aoAbrir;
  final VoidCallback aoTentarNovamente;

  @override
  Widget build(BuildContext context) {
    return ConteudoCarga<CatalogoMateriais>(
      carga: carga,
      rotuloCarregando: 'Carregando materiais…',
      aoTentarNovamente: aoTentarNovamente,
      construir: (context, catalogo) {
        if (catalogo.itens.isEmpty) {
          return const EstadoVazio(icone: Icons.recycling, titulo: 'Nenhum material ativo no catálogo');
        }
        // Os quatro tradicionais primeiro, depois os demais na ordem da API.
        int ordem(MaterialColeta m) {
          final i = materiaisTradicionais.indexWhere((n) => normalizarNome(n) == normalizarNome(m.nome));
          return i < 0 ? 99 : i;
        }

        final itens = [...catalogo.itens]..sort((a, b) => ordem(a).compareTo(ordem(b)));
        return GradeResponsiva(
          larguraMinima: 300,
          filhos: [
            for (var i = 0; i < itens.length; i++)
              Revelar(
                atraso: Duration(milliseconds: 60 * (i % 3)),
                child: CartaoMaterial(material: itens[i], aoAbrir: () => aoAbrir(itens[i].id)),
              ),
          ],
        );
      },
    );
  }
}

class _Recolhimento extends StatelessWidget {
  const _Recolhimento({required this.navegar});

  final NavegacaoInicio navegar;

  static const _jornada = [
    'Escolha um ponto que ofereça recolhimento domiciliar.',
    'Informe os materiais, as quantidades e o endereço.',
    'O operador do ponto agenda a data da coleta.',
    'Acompanhe tudo pelo protocolo da solicitação.',
  ];

  @override
  Widget build(BuildContext context) {
    final texto = Theme.of(context).textTheme;
    final conteudo = Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Semantics(header: true, child: Text('Recolhimento domiciliar', style: texto.headlineMedium)),
        const SizedBox(height: Espaco.xs),
        Text(
          'Alguns pontos buscam os recicláveis na sua casa. Você pede, o ponto agenda e você acompanha.',
          style: texto.bodyLarge?.copyWith(color: Cores.textoSecundario),
        ),
        const SizedBox(height: Espaco.l),
        for (var i = 0; i < _jornada.length; i++)
          Revelar(
            atraso: Duration(milliseconds: 80 * i),
            child: Padding(
              padding: const EdgeInsets.only(bottom: Espaco.s),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  CircleAvatar(
                    radius: 14,
                    backgroundColor: Cores.primario,
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
          ),
        const SizedBox(height: Espaco.s),
        FilledButton.icon(
          onPressed: navegar.solicitar,
          icon: const Icon(Icons.local_shipping_outlined),
          label: const Text('Solicitar recolhimento'),
        ),
      ],
    );
    return LayoutBuilder(
      builder: (context, restricoes) {
        final ilustracao = Revelar(
          child: Container(
            decoration: BoxDecoration(borderRadius: BorderRadius.circular(24), boxShadow: Sombra.elevada),
            child: const IlustracaoRecolhimento(),
          ),
        );
        if (restricoes.maxWidth < 840) {
          return Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              ilustracao,
              const SizedBox(height: Espaco.l),
              conteudo,
            ],
          );
        }
        return Row(
          children: [
            Expanded(child: ilustracao),
            const SizedBox(width: Espaco.xxl),
            Expanded(child: conteudo),
          ],
        );
      },
    );
  }
}

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

  void _buscar([String? texto]) {
    final nome = (texto ?? _nome.text).trim();
    widget.navegar.pontos(FiltroPontos(nome: nome.isEmpty ? null : nome));
  }

  @override
  Widget build(BuildContext context) {
    return Revelar(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          BarraPesquisa(
            controlador: _nome,
            aoPesquisar: _buscar,
            aoAbrirFiltros: () => widget.navegar.pontos(const FiltroPontos()),
          ),
          const SizedBox(height: Espaco.m),
          AtalhosMateriais(
            catalogo: widget.materiais,
            selecionado: null,
            aoSelecionar: (m) => widget.navegar.pontos(FiltroPontos(materialId: m?.id)),
          ),
          const SizedBox(height: Espaco.m),
          Align(
            alignment: Alignment.centerLeft,
            child: FilledButton.icon(
              onPressed: _buscar,
              icon: const Icon(Icons.search),
              label: const Text('Abrir o catálogo de pontos'),
            ),
          ),
        ],
      ),
    );
  }
}

class _Encerramento extends StatelessWidget {
  const _Encerramento({required this.navegar});

  final NavegacaoInicio navegar;

  @override
  Widget build(BuildContext context) {
    final texto = Theme.of(context).textTheme;
    final estiloClaro = OutlinedButton.styleFrom(
      foregroundColor: Colors.white,
      side: const BorderSide(color: Colors.white),
    );
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: Espaco.m, vertical: 64),
      decoration: const BoxDecoration(gradient: LinearGradient(colors: [Cores.primario, Color(0xFF1F7F5C)])),
      child: Center(
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 720),
          child: Revelar(
            child: Column(
              children: [
                Semantics(
                  header: true,
                  child: Text(
                    'Pronto para separar e reciclar?',
                    style: texto.headlineMedium?.copyWith(color: Colors.white),
                    textAlign: TextAlign.center,
                  ),
                ),
                const SizedBox(height: Espaco.s),
                Text(
                  'Encontre o ponto mais conveniente pelo nome, município ou material.',
                  style: texto.bodyLarge?.copyWith(color: Colors.white),
                  textAlign: TextAlign.center,
                ),
                const SizedBox(height: Espaco.l),
                Wrap(
                  alignment: WrapAlignment.center,
                  spacing: Espaco.s,
                  runSpacing: Espaco.s,
                  children: [
                    FilledButton(
                      style: FilledButton.styleFrom(backgroundColor: Colors.white, foregroundColor: Cores.primario),
                      onPressed: () => navegar.pontos(const FiltroPontos()),
                      child: const Text('Ver pontos de coleta'),
                    ),
                    if (navegar.demonstracao != null)
                      OutlinedButton(
                        style: estiloClaro,
                        onPressed: navegar.demonstracao,
                        child: const Text('Experimentar demonstração'),
                      ),
                  ],
                ),
              ],
            ),
          ),
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
      color: Cores.superficie,
      padding: const EdgeInsets.symmetric(horizontal: Espaco.m, vertical: Espaco.l),
      child: Center(
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: Largura.catalogo),
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
                  Text('Projeto acadêmico · Programação Web I · IFS', style: texto.bodySmall),
                ],
              ),
              Wrap(
                spacing: Espaco.xs,
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
      ),
    );
  }
}
