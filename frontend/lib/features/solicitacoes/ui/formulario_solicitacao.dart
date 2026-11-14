import 'package:material_ui/material_ui.dart';

import '../../../app/tema/tokens.dart';
import '../../../core/api/falhas.dart';
import '../../../core/dominio/enums.dart';
import '../../../core/dominio/quantidade.dart';
import '../../../ui/compartilhado/campos.dart';
import '../../../ui/compartilhado/dialogos.dart';
import '../../../ui/compartilhado/envio.dart';
import '../../../ui/compartilhado/estados.dart';
import '../../../ui/compartilhado/layout.dart';
import '../../../ui/compartilhado/material_visual.dart';
import '../../catalogo/data/modelos.dart';
import '../../catalogo/data/repositorio_catalogo.dart';
import '../../moradores/data/moradores.dart';
import '../../moradores/ui/formulario_endereco.dart';
import '../data/solicitacoes.dart';
import '../../../ui/compartilhado/superficie.dart';

/// Linha de item em edição. O texto digitado é preservado entre erros.
class LinhaItem {
  LinhaItem({this.materialId, String quantidade = ''}) : quantidade = TextEditingController(text: quantidade);

  int? materialId;
  final TextEditingController quantidade;
  final chave = UniqueKey();
}

/// Formulário de nova solicitação (RF08) ou de correção pelo dono
/// enquanto `SOLICITADA` (RN04), com versão (RN06).
class PaginaFormularioSolicitacao extends StatefulWidget {
  const PaginaFormularioSolicitacao({
    super.key,
    required this.moradorId,
    required this.solicitacoes,
    required this.catalogo,
    required this.moradores,
    required this.aoConcluir,
    required this.aoVoltar,
    required this.aoConsultarPontos,
    this.pontoInicial,
    this.enderecoPreferido,
    this.solicitacaoId,
  });

  final int moradorId;
  final RepositorioSolicitacoes solicitacoes;
  final RepositorioCatalogo catalogo;
  final RepositorioMoradores moradores;
  final void Function(SolicitacaoDetalhe resultado, {required bool criada}) aoConcluir;
  final VoidCallback aoVoltar;
  final VoidCallback aoConsultarPontos;
  final int? pontoInicial;
  final int? enderecoPreferido;

  /// Presente na correção.
  final int? solicitacaoId;

  @override
  State<PaginaFormularioSolicitacao> createState() => _PaginaFormularioSolicitacaoState();
}

class _PaginaFormularioSolicitacaoState extends State<PaginaFormularioSolicitacao> {
  final _form = GlobalKey<FormState>();
  final _envio = EstadoEnvio();
  final _observacoes = TextEditingController();
  final _linhas = <LinhaItem>[];

  bool get _correcao => widget.solicitacaoId != null;

  bool _carregando = true;
  FalhaApi? _falhaCarga;
  PontoColeta? _ponto;
  List<PontoColeta> _pontosDomiciliares = const [];
  List<EnderecoMorador> _enderecos = const [];
  int? _enderecoId;
  CatalogoMateriais? _materiais;
  SolicitacaoDetalhe? _original;
  String? _avisoVersao;

  /// Itens capturados no último envio, para associar `itens[i]` às linhas.
  List<LinhaItem> _enviadas = const [];

  @override
  void initState() {
    super.initState();
    _carregar();
  }

  @override
  void dispose() {
    _envio.dispose();
    _observacoes.dispose();
    for (final l in _linhas) {
      l.quantidade.dispose();
    }
    super.dispose();
  }

  Future<void> _carregar() async {
    setState(() {
      _carregando = true;
      _falhaCarga = null;
    });
    try {
      final enderecos = widget.moradores.enderecos(widget.moradorId);
      final materiais = widget.catalogo.materiaisAtivos();
      int? pontoId = widget.pontoInicial;
      if (_correcao) {
        final original = await widget.solicitacoes.detalhe(widget.solicitacaoId!);
        _original = original;
        pontoId = original.pontoId;
        _observacoes.text = original.observacoes ?? '';
        _linhas
          ..clear()
          ..addAll([
            for (final i in original.itens) LinhaItem(materialId: i.materialId, quantidade: i.quantidadeEstimada.texto),
          ]);
      } else {
        final pagina = await widget.catalogo.pesquisarPontos(const FiltroPontos(), tamanho: 100);
        _pontosDomiciliares = pagina.conteudo.where((p) => p.aceitaSolicitacao).toList();
        if (_linhas.isEmpty) _linhas.add(LinhaItem());
      }
      _enderecos = await enderecos;
      _materiais = await materiais;
      if (pontoId != null) await _usarPonto(pontoId, notificar: false);
      if (!_correcao && _enderecoId == null && _enderecos.isNotEmpty) {
        _enderecoId = _enderecos.any((e) => e.id == widget.enderecoPreferido)
            ? widget.enderecoPreferido
            : _enderecos.first.id;
      }
    } on FalhaApi catch (e) {
      _falhaCarga = e;
    }
    if (mounted) setState(() => _carregando = false);
  }

  Future<void> _usarPonto(int id, {bool notificar = true, bool forcar = false}) async {
    try {
      _ponto = await widget.catalogo.ponto(id, forcar: forcar);
    } on ErroHttp catch (e) {
      if (!e.naoEncontrado) rethrow;
      _ponto = null;
      _envio
        ..mensagem = 'O ponto escolhido não está mais disponível. Escolha outro ponto.'
        ..tipoMensagem = TipoAviso.alerta;
    }
    if (notificar && mounted) setState(() {});
  }

  /// Materiais do ponto confirmados como ativos no catálogo. Se a página de
  /// ativos não for completa, o vínculo é oferecido e a API decide.
  List<MaterialAceito> get _materiaisDisponiveis {
    final p = _ponto;
    if (p == null) return const [];
    final cat = _materiais;
    return p.materiaisAceitos.where((v) => cat == null || cat.porId(v.materialId) != null || !cat.completo).toList();
  }

  MaterialAceito? _vinculo(int? materialId) =>
      _ponto?.materiaisAceitos.where((v) => v.materialId == materialId).firstOrNull;

  String? _problemaLinha(LinhaItem linha) {
    if (linha.materialId == null) return null;
    final disponivel = _materiaisDisponiveis.any((v) => v.materialId == linha.materialId);
    if (!disponivel) return 'Este material não está disponível neste ponto. Remova a linha ou escolha outro material.';
    return null;
  }

  Future<void> _cadastrarEndereco() async {
    final criado = await abrirFormulario<EnderecoMorador>(
      context,
      titulo: 'Cadastrar endereço',
      construir: (_) => FormularioEndereco(
        rotuloSalvar: 'Cadastrar endereço',
        salvar: (e) => widget.moradores.criarEndereco(widget.moradorId, e),
      ),
    );
    if (criado == null || !mounted) return;
    setState(() {
      _enderecos = [..._enderecos, criado];
      _enderecoId = criado.id;
    });
  }

  Future<void> _enviar() async {
    final valido = _form.currentState!.validate();
    if (_ponto == null || _enderecoId == null || _linhas.isEmpty || !valido) {
      setState(() {
        _envio
          ..mensagem = _linhas.isEmpty
              ? 'Inclua pelo menos um material.'
              : 'Revise os campos destacados antes de enviar.'
          ..tipoMensagem = TipoAviso.erro;
      });
      return;
    }
    if (_linhas.any((l) => _problemaLinha(l) != null)) {
      setState(() {
        _envio
          ..mensagem = 'Há materiais indisponíveis neste ponto. Remova ou ajuste as linhas indicadas.'
          ..tipoMensagem = TipoAviso.alerta;
      });
      return;
    }
    _enviadas = List.of(_linhas);
    final itens = [
      for (final l in _enviadas)
        ItemPedido(
          l.materialId!,
          LeituraQuantidade.de(l.quantidade.text, unidade: _vinculo(l.materialId)?.unidade ?? Unidade.kg).valor!,
        ),
    ];
    final resultado = await _envio.executar(
      () => _correcao
          ? widget.solicitacoes.corrigir(
              widget.solicitacaoId!,
              enderecoId: _enderecoId!,
              itens: itens,
              versao: _original!.versao,
              observacoes: _observacoes.text,
            )
          : widget.solicitacoes.criar(
              pontoId: _ponto!.id,
              enderecoId: _enderecoId!,
              itens: itens,
              observacoes: _observacoes.text,
            ),
    );
    if (resultado != null) {
      widget.aoConcluir(resultado, criada: !_correcao);
      return;
    }
    await _tratarFalha();
  }

  Future<void> _tratarFalha() async {
    final falha = _envio.falha;
    if (falha is ErroHttp && falha.conflitoVersao && _correcao) {
      // Relê para obter a versão e a situação atuais, preservando as edições.
      try {
        final atual = await widget.solicitacoes.detalhe(widget.solicitacaoId!);
        _original = atual;
        _avisoVersao = atual.situacao == SituacaoSolicitacao.solicitada
            ? 'A solicitação foi alterada enquanto você editava. Seus dados foram mantidos: revise e envie novamente.'
            : 'A solicitação mudou para ${atual.situacao.rotulo} e não pode mais ser corrigida.';
      } on FalhaApi catch (e) {
        _avisoVersao = e.mensagem;
      }
    } else if (falha is ErroHttp && falha.conflitoRegra && _ponto != null) {
      // Disponibilidade mudou: atualiza o ponto e o catálogo de ativos.
      widget.catalogo.invalidar();
      try {
        _materiais = await widget.catalogo.materiaisAtivos();
        await _usarPonto(_ponto!.id, forcar: true, notificar: false);
      } on FalhaApi {
        // Mantém os dados anteriores; a mensagem do servidor já orienta.
      }
    } else if (falha is FalhaRede || falha is FalhaPrazo) {
      _envio.mensagem = '${falha!.mensagem} Confira em Minhas solicitações antes de enviar de novo.';
    }
    if (mounted) setState(() {});
  }

  String? _erroLinha(int indice, String campo) {
    final linha = _linhas[indice];
    final posicao = _enviadas.indexOf(linha);
    if (posicao < 0) return null;
    return _envio.erro('itens[$posicao].$campo');
  }

  @override
  Widget build(BuildContext context) {
    final titulo = _correcao ? 'Corrigir solicitação' : 'Nova solicitação';
    if (_carregando) {
      return PaginaRolavel(larguraMaxima: Largura.formulario, filhos: [TituloPagina(titulo), const EstadoCarregando()]);
    }
    if (_falhaCarga != null) {
      return PaginaRolavel(
        larguraMaxima: Largura.formulario,
        filhos: [
          TituloPagina(titulo),
          EstadoErro(mensagem: _falhaCarga!.mensagem, aoTentarNovamente: _carregar),
          TextButton(onPressed: widget.aoVoltar, child: const Text('Voltar')),
        ],
      );
    }
    final original = _original;
    if (_correcao && original != null && original.situacao != SituacaoSolicitacao.solicitada) {
      return PaginaRolavel(
        larguraMaxima: Largura.formulario,
        filhos: [
          TituloPagina(titulo),
          Aviso(
            tipo: TipoAviso.alerta,
            mensagem: _avisoVersao ?? 'Só é possível corrigir solicitações na situação Solicitada.',
          ),
          const SizedBox(height: Espaco.m),
          Align(
            alignment: Alignment.centerLeft,
            child: Elevavel(
              child: FilledButton(onPressed: widget.aoVoltar, child: const Text('Voltar à solicitação')),
            ),
          ),
        ],
      );
    }
    return ListenableBuilder(
      listenable: _envio,
      builder: (context, _) => Form(
        key: _form,
        child: PaginaRolavel(
          larguraMaxima: Largura.formulario,
          filhos: [
            Align(
              alignment: Alignment.centerLeft,
              child: TextButton.icon(
                onPressed: widget.aoVoltar,
                icon: const Icon(Icons.arrow_back),
                label: const Text('Voltar'),
              ),
            ),
            TituloPagina(
              titulo,
              subtitulo: _correcao
                  ? 'Protocolo ${original?.protocolo}. O ponto não muda; endereço, materiais e observações podem ser corrigidos.'
                  : 'O ponto combina a data do recolhimento depois do envio.',
            ),
            const LegendaObrigatorio(),
            if (_avisoVersao != null) ...[
              Aviso(tipo: TipoAviso.alerta, mensagem: _avisoVersao!),
              const SizedBox(height: Espaco.m),
            ],
            MensagemEnvio(estado: _envio),
            _secao('Ponto e endereço', [_campoPonto(), const SizedBox(height: Espaco.m), _campoEndereco()]),
            _secao('Materiais e quantidades', _camposItens()),
            _secao('Observações', [
              CampoTexto(
                controlador: _observacoes,
                rotulo: 'Observações para o ponto',
                limite: 500,
                linhas: 3,
                ajuda: 'Por exemplo, onde os materiais estarão.',
                erroServidor: _envio.erro('observacoes'),
              ),
            ]),
            const SizedBox(height: Espaco.l),
            Wrap(
              alignment: WrapAlignment.end,
              spacing: Espaco.s,
              runSpacing: Espaco.s,
              children: [
                TextButton(onPressed: widget.aoVoltar, child: const Text('Voltar')),
                BotaoEnvio(
                  rotulo: _correcao ? 'Salvar correção' : 'Enviar solicitação',
                  enviando: _envio.enviando,
                  aoPressionar: _enviar,
                  icone: Icons.send_outlined,
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _secao(String titulo, List<Widget> filhos) => Column(
    crossAxisAlignment: CrossAxisAlignment.stretch,
    children: [
      TituloSecao(titulo),
      Card(
        child: Padding(
          padding: const EdgeInsets.all(Espaco.m),
          child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: filhos),
        ),
      ),
    ],
  );

  Widget _campoPonto() {
    final texto = Theme.of(context).textTheme;
    final ponto = _ponto;
    if (_correcao || (widget.pontoInicial != null && ponto != null)) {
      return MergeSemantics(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Ponto de coleta', style: texto.bodySmall),
            Text(ponto?.nome ?? 'Ponto #${_original?.pontoId}', style: texto.titleMedium),
            if (ponto != null) Text('${ponto.endereco.municipio} · ${ponto.endereco.bairro}', style: texto.bodyMedium),
            if (ponto != null && !ponto.aceitaSolicitacao)
              Text(
                'Este ponto não aceita novas solicitações de recolhimento domiciliar.',
                style: texto.bodyMedium?.copyWith(color: Cores.alerta),
              ),
          ],
        ),
      );
    }
    if (_pontosDomiciliares.isEmpty) {
      return Aviso(
        tipo: TipoAviso.alerta,
        mensagem: 'Nenhum ponto ativo oferece recolhimento domiciliar no momento.',
        acao: TextButton(onPressed: widget.aoConsultarPontos, child: const Text('Consultar pontos')),
      );
    }
    return DropdownButtonFormField<int>(
      initialValue: ponto?.id,
      isExpanded: true,
      decoration: InputDecoration(labelText: 'Ponto de coleta *', errorText: _envio.erro('pontoId')),
      items: [
        for (final p in _pontosDomiciliares)
          DropdownMenuItem(
            value: p.id,
            child: Text('${p.nome} · ${p.endereco.municipio}', overflow: TextOverflow.ellipsis),
          ),
      ],
      validator: (v) => v == null ? 'Escolha o ponto de coleta.' : null,
      onChanged: (id) {
        if (id != null) _usarPonto(id);
      },
    );
  }

  Widget _campoEndereco() {
    final texto = Theme.of(context).textTheme;
    final original = _original;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        if (original != null) ...[
          Text('Endereço registrado na solicitação', style: texto.bodySmall),
          Text(original.enderecoCopia, style: texto.bodyMedium),
          const SizedBox(height: Espaco.s),
          Text('Escolha o endereço atual para a correção.', style: texto.bodySmall),
          const SizedBox(height: Espaco.xs),
        ],
        if (_enderecos.isEmpty)
          const Aviso(tipo: TipoAviso.info, mensagem: 'Cadastre um endereço para receber o recolhimento.')
        else
          DropdownButtonFormField<int>(
            initialValue: _enderecoId,
            isExpanded: true,
            decoration: InputDecoration(labelText: 'Endereço do recolhimento *', errorText: _envio.erro('enderecoId')),
            items: [
              for (final e in _enderecos)
                DropdownMenuItem(
                  value: e.id,
                  child: Text('${e.endereco.linha1} · ${e.endereco.municipio}', overflow: TextOverflow.ellipsis),
                ),
            ],
            validator: (v) => v == null ? 'Escolha o endereço.' : null,
            onChanged: (v) => setState(() => _enderecoId = v),
          ),
        Align(
          alignment: Alignment.centerLeft,
          child: TextButton.icon(
            onPressed: _cadastrarEndereco,
            icon: const Icon(Icons.add_location_alt_outlined),
            label: const Text('Cadastrar endereço'),
          ),
        ),
      ],
    );
  }

  List<Widget> _camposItens() {
    final disponiveis = _materiaisDisponiveis;
    final erroItens = _envio.erro('itens');
    return [
      if (_ponto == null)
        const Text('Escolha o ponto para ver os materiais aceitos.')
      else if (disponiveis.isEmpty)
        const Aviso(tipo: TipoAviso.alerta, mensagem: 'Este ponto não tem materiais disponíveis para solicitação.'),
      if (erroItens != null) ...[Aviso(tipo: TipoAviso.erro, mensagem: erroItens), const SizedBox(height: Espaco.s)],
      for (var i = 0; i < _linhas.length; i++) _linha(i, disponiveis),
      if (_ponto != null && _linhas.length < disponiveis.length)
        Align(
          alignment: Alignment.centerLeft,
          child: OutlinedButton.icon(
            onPressed: () => setState(() => _linhas.add(LinhaItem())),
            icon: const Icon(Icons.add),
            label: const Text('Adicionar material'),
          ),
        ),
    ];
  }

  Widget _linha(int i, List<MaterialAceito> disponiveis) {
    final linha = _linhas[i];
    final vinculo = _vinculo(linha.materialId);
    final usados = {
      for (final l in _linhas)
        if (l != linha && l.materialId != null) l.materialId,
    };
    final opcoes = disponiveis.where((v) => !usados.contains(v.materialId)).toList();
    final problema = _problemaLinha(linha);
    final atualForaDasOpcoes = linha.materialId != null && !opcoes.any((v) => v.materialId == linha.materialId);
    final unidade = vinculo?.unidade ?? Unidade.desconhecida;
    final ajuda = vinculo == null
        ? null
        : [
            'Em ${unidade.extenso}',
            if (vinculo.limite != null) 'limite ${vinculo.limite!.comUnidade(unidade)} por solicitação',
          ].join(' · ');
    return Padding(
      key: linha.chave,
      padding: const EdgeInsets.only(bottom: Espaco.m),
      child: Container(
        padding: const EdgeInsets.all(Espaco.s),
        decoration: BoxDecoration(
          border: Border.all(color: problema == null ? Cores.divisor : Cores.alerta),
          borderRadius: BorderRadius.circular(Raio.campo),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Row(
              children: [
                Expanded(child: Text('Material ${i + 1}', style: Theme.of(context).textTheme.titleSmall)),
                IconButton(
                  tooltip: 'Remover material ${i + 1}',
                  icon: const Icon(Icons.delete_outline),
                  onPressed: () => setState(() {
                    _linhas.removeAt(i);
                    linha.quantidade.dispose();
                  }),
                ),
              ],
            ),
            if (problema != null) ...[
              Text(problema, style: Theme.of(context).textTheme.bodySmall?.copyWith(color: Cores.alerta)),
              const SizedBox(height: Espaco.xs),
            ],
            LinhaCampos(
              flex: const [3, 2],
              filhos: [
                Padding(
                  padding: const EdgeInsets.only(bottom: Espaco.m),
                  child: DropdownButtonFormField<int>(
                    initialValue: linha.materialId,
                    isExpanded: true,
                    decoration: InputDecoration(labelText: 'Material *', errorText: _erroLinha(i, 'materialId')),
                    items: [
                      if (atualForaDasOpcoes)
                        DropdownMenuItem(
                          value: linha.materialId,
                          child: Text('${_nomeMaterial(linha.materialId!)} (indisponível)'),
                        ),
                      for (final v in opcoes)
                        DropdownMenuItem(
                          value: v.materialId,
                          child: Row(
                            children: [
                              IconeMaterial(nome: v.nomeMaterial, tamanho: 20),
                              const SizedBox(width: Espaco.xs),
                              Flexible(child: Text(v.nomeMaterial, overflow: TextOverflow.ellipsis)),
                            ],
                          ),
                        ),
                    ],
                    validator: (v) => v == null ? 'Escolha o material.' : null,
                    onChanged: (v) => setState(() => linha.materialId = v),
                  ),
                ),
                CampoTexto(
                  controlador: linha.quantidade,
                  rotulo: unidade.simbolo.isEmpty ? 'Quantidade estimada' : 'Quantidade estimada (${unidade.simbolo})',
                  obrigatorio: true,
                  ajuda: ajuda,
                  teclado: const TextInputType.numberWithOptions(decimal: true),
                  erroServidor: _erroLinha(i, 'quantidadeEstimada'),
                  validar: (texto) {
                    if (linha.materialId == null) return null;
                    final leitura = LeituraQuantidade.de(texto, unidade: unidade);
                    if (!leitura.valida) return leitura.erro;
                    final limite = vinculo?.limite;
                    if (limite != null && leitura.valor!.compareTo(limite) > 0) {
                      return 'O limite deste ponto é ${limite.comUnidade(unidade)}.';
                    }
                    return null;
                  },
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  String _nomeMaterial(int id) =>
      _original?.itens.where((i) => i.materialId == id).firstOrNull?.nomeMaterial ??
      _materiais?.porId(id)?.nome ??
      'Material #$id';
}
