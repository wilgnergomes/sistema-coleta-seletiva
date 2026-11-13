import 'package:material_ui/material_ui.dart';

import '../../../app/sessao.dart';
import '../../../app/tema/tokens.dart';
import '../../../core/api/falhas.dart';
import '../../../core/estado/carga.dart';
import '../../../ui/compartilhado/layout.dart';
import '../../catalogo/data/modelos.dart';
import '../../catalogo/data/repositorio_catalogo.dart';
import '../../catalogo/ui/componentes_catalogo.dart';
import '../data/moradores.dart';

class InicioMoradorViewModel extends ChangeNotifier {
  InicioMoradorViewModel({required this.sessao, required this.moradores, required this.catalogo});

  final Sessao sessao;
  final RepositorioMoradores moradores;
  final RepositorioCatalogo catalogo;
  final enderecos = Carga<List<EnderecoMorador>>();
  final materiais = Carga<CatalogoMateriais>();

  void carregar() {
    enderecos.executar(() => moradores.enderecos(sessao.usuarioId!), notifyListeners);
    materiais.executar(catalogo.materiaisAtivos, notifyListeners);
  }

  /// Endereço de referência: o escolhido na sessão ou o primeiro cadastrado.
  EnderecoMorador? get selecionado {
    final lista = enderecos.dados;
    if (lista == null || lista.isEmpty) return null;
    return lista.where((e) => e.id == sessao.enderecoSelecionadoId).firstOrNull ?? lista.first;
  }

  String? get municipio => selecionado?.endereco.municipio;

  void selecionar(EnderecoMorador e) {
    sessao.enderecoSelecionadoId = e.id;
    notifyListeners();
  }
}

/// Início do morador: descoberta de pontos a partir do endereço
/// selecionado e atalhos para as tarefas principais.
///
/// O endereço selecionado não é localização: serve apenas de referência de
/// município para a pesquisa.
class PaginaInicioMorador extends StatefulWidget {
  const PaginaInicioMorador({
    super.key,
    required this.viewModel,
    required this.aoPesquisar,
    required this.aoGerenciarEnderecos,
    this.extras,
  });

  final InicioMoradorViewModel viewModel;
  final ValueChanged<FiltroPontos> aoPesquisar;
  final VoidCallback aoGerenciarEnderecos;

  /// Seções adicionais, que recebem o município do endereço selecionado.
  final Widget Function(BuildContext context, String? municipio)? extras;

  @override
  State<PaginaInicioMorador> createState() => _PaginaInicioMoradorState();
}

class _PaginaInicioMoradorState extends State<PaginaInicioMorador> {
  final _nome = TextEditingController();

  InicioMoradorViewModel get _vm => widget.viewModel;

  @override
  void initState() {
    super.initState();
    _vm.carregar();
  }

  @override
  void dispose() {
    _nome.dispose();
    super.dispose();
  }

  void _pesquisar({String? nome, int? materialId}) {
    final texto = (nome ?? '').trim();
    widget.aoPesquisar(
      FiltroPontos(nome: texto.isEmpty ? null : texto, municipio: _vm.municipio, materialId: materialId),
    );
  }

  Future<void> _abrirFiltros() async {
    final f = await abrirFiltrosPontos(
      context,
      atual: FiltroPontos(nome: _nome.text.trim().isEmpty ? null : _nome.text.trim(), municipio: _vm.municipio),
      catalogo: _vm.materiais.dados,
    );
    if (f != null) widget.aoPesquisar(f);
  }

  Future<void> _escolherEndereco() async {
    final lista = _vm.enderecos.dados ?? const [];
    final escolhido = await showModalBottomSheet<Object>(
      context: context,
      showDragHandle: true,
      builder: (context) => SafeArea(
        child: ListView(
          shrinkWrap: true,
          children: [
            Padding(
              padding: const EdgeInsets.fromLTRB(Espaco.l, 0, Espaco.l, Espaco.s),
              child: Semantics(
                header: true,
                child: Text('Endereço de referência', style: Theme.of(context).textTheme.titleLarge),
              ),
            ),
            for (final e in lista)
              ListTile(
                leading: Icon(e.id == _vm.selecionado?.id ? Icons.radio_button_checked : Icons.radio_button_off),
                title: Text(e.endereco.linha1),
                subtitle: Text('${e.endereco.bairro} · ${e.endereco.municipio}/${e.endereco.uf}'),
                selected: e.id == _vm.selecionado?.id,
                onTap: () => Navigator.of(context).pop(e),
              ),
            ListTile(
              leading: const Icon(Icons.edit_location_alt_outlined),
              title: const Text('Gerenciar endereços'),
              onTap: () => Navigator.of(context).pop('gerenciar'),
            ),
          ],
        ),
      ),
    );
    if (escolhido is EnderecoMorador) _vm.selecionar(escolhido);
    if (escolhido == 'gerenciar') widget.aoGerenciarEnderecos();
  }

  @override
  Widget build(BuildContext context) {
    final texto = Theme.of(context).textTheme;
    return ListenableBuilder(
      listenable: _vm,
      builder: (context, _) {
        final nome = _vm.sessao.ator?.nome;
        final municipio = _vm.municipio;
        return PaginaRolavel(
          larguraMaxima: Largura.catalogo,
          filhos: [
            _SeletorEndereco(
              carregando: _vm.enderecos.dados == null && _vm.enderecos.falha == null,
              falha: _vm.enderecos.falha,
              selecionado: _vm.selecionado,
              aoTocar: (_vm.enderecos.dados ?? const []).isEmpty ? widget.aoGerenciarEnderecos : _escolherEndereco,
              aoTentarNovamente: _vm.carregar,
            ),
            const SizedBox(height: Espaco.l),
            Semantics(
              header: true,
              child: Text(
                nome == null ? 'O que você quer fazer?' : 'Olá, ${nome.split(' ').first}',
                style: texto.headlineSmall,
              ),
            ),
            const SizedBox(height: Espaco.m),
            BarraPesquisa(
              controlador: _nome,
              aoPesquisar: (t) => _pesquisar(nome: t),
              aoAbrirFiltros: _abrirFiltros,
            ),
            const SizedBox(height: Espaco.l),
            Row(
              children: [
                Expanded(
                  child: Semantics(
                    header: true,
                    child: Text(
                      municipio == null ? 'Pontos por material' : 'Pontos em $municipio',
                      style: texto.titleLarge,
                    ),
                  ),
                ),
                TextButton(
                  onPressed: () => _pesquisar(),
                  child: Text(municipio == null ? 'Ver todos os pontos' : 'Ver todos em $municipio'),
                ),
              ],
            ),
            const SizedBox(height: Espaco.xs),
            AtalhosMateriais(
              catalogo: _vm.materiais.dados,
              selecionado: null,
              aoSelecionar: (m) => _pesquisar(materialId: m?.id),
            ),
            if (widget.extras != null && (_vm.enderecos.dados != null || _vm.enderecos.falha != null))
              widget.extras!(context, municipio),
          ],
        );
      },
    );
  }
}

/// Cartão arredondado compacto com o endereço de referência.
class _SeletorEndereco extends StatelessWidget {
  const _SeletorEndereco({
    required this.carregando,
    required this.falha,
    required this.selecionado,
    required this.aoTocar,
    required this.aoTentarNovamente,
  });

  final bool carregando;
  final FalhaApi? falha;
  final EnderecoMorador? selecionado;
  final VoidCallback aoTocar;
  final VoidCallback aoTentarNovamente;

  @override
  Widget build(BuildContext context) {
    final texto = Theme.of(context).textTheme;
    final (rotulo, valor, acao) = switch ((carregando, falha, selecionado)) {
      (true, _, _) => ('Endereço selecionado', 'Carregando endereços…', null),
      (_, final FalhaApi _, _) => ('Endereços indisponíveis', 'Toque para tentar novamente', aoTentarNovamente),
      (_, _, null) => ('Nenhum endereço cadastrado', 'Cadastrar endereço', aoTocar),
      (_, _, final EnderecoMorador e) => (
        'Endereço selecionado',
        '${e.endereco.logradouro}, ${e.endereco.numero} · ${e.endereco.municipio}',
        aoTocar,
      ),
    };
    return Align(
      alignment: Alignment.centerLeft,
      child: Semantics(
        button: acao != null,
        label: '$rotulo: $valor',
        excludeSemantics: true,
        child: Material(
          color: Cores.superficie,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(Raio.pilula),
            side: const BorderSide(color: Cores.divisor),
          ),
          child: InkWell(
            onTap: acao,
            customBorder: const StadiumBorder(),
            child: ConstrainedBox(
              constraints: const BoxConstraints(minHeight: 48, maxWidth: 520),
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: Espaco.m, vertical: Espaco.xs),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    const Icon(Icons.home_outlined, color: Cores.primario, size: 20),
                    const SizedBox(width: Espaco.xs),
                    Flexible(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Text(rotulo, style: texto.bodySmall),
                          Text(valor, style: texto.labelLarge, maxLines: 2, overflow: TextOverflow.ellipsis),
                        ],
                      ),
                    ),
                    if (acao != null) ...[const SizedBox(width: Espaco.xxs), const Icon(Icons.expand_more, size: 20)],
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
