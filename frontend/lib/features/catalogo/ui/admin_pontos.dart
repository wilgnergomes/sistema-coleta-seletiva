import 'package:material_ui/material_ui.dart';

import '../../../app/tema/tokens.dart';
import '../../../core/api/cliente_api.dart';
import '../../../core/api/falhas.dart';
import '../../../core/api/pagina.dart';
import '../../../core/dominio/enums.dart';
import '../../../core/dominio/quantidade.dart';
import '../../../core/estado/carga.dart';
import '../../../ui/compartilhado/campos.dart';
import '../../../ui/compartilhado/dialogos.dart';
import '../../../ui/compartilhado/envio.dart';
import '../../../ui/compartilhado/estados.dart';
import '../../../ui/compartilhado/layout.dart';
import '../../../ui/compartilhado/material_visual.dart';
import '../../../ui/compartilhado/paginador.dart';
import '../../moradores/ui/formulario_endereco.dart';
import '../data/modelos.dart';
import '../data/repositorio_catalogo.dart';
import '../../../ui/compartilhado/superficie.dart';

/// Editor do ponto (RF04) com a seção de materiais aceitos (RF05). Em
/// conflito da RN05, mostra a mensagem do servidor e o atalho para as
/// solicitações do ponto.
class PaginaEditorPonto extends StatefulWidget {
  const PaginaEditorPonto({
    super.key,
    required this.repositorio,
    required this.aoVoltar,
    required this.aoCriado,
    required this.aoVerSolicitacoes,
    this.id,
  });

  final RepositorioCatalogo repositorio;
  final int? id;
  final VoidCallback aoVoltar;
  final ValueChanged<PontoColeta> aoCriado;
  final ValueChanged<int> aoVerSolicitacoes;

  @override
  State<PaginaEditorPonto> createState() => _PaginaEditorPontoState();
}

class _PaginaEditorPontoState extends State<PaginaEditorPonto> {
  final _form = GlobalKey<FormState>();
  final _nome = TextEditingController();
  final _contato = TextEditingController();
  final _horarios = TextEditingController();
  CamposEndereco _endereco = CamposEndereco();
  bool _domiciliar = false;
  SituacaoCadastro _situacao = SituacaoCadastro.ativo;
  PontoColeta? _ponto;
  final _carga = Carga<PontoColeta>();
  final _envio = EstadoEnvio();

  @override
  void initState() {
    super.initState();
    if (widget.id != null) _carregar();
  }

  Future<void> _carregar() async {
    await _carga.executar(() => widget.repositorio.ponto(widget.id!, acesso: Acesso.ator, forcar: true), () {
      if (mounted) setState(() {});
    });
    final p = _carga.dados;
    if (p != null && mounted) _preencher(p);
  }

  void _preencher(PontoColeta p) {
    setState(() {
      _ponto = p;
      _nome.text = p.nome;
      _contato.text = p.contato ?? '';
      _horarios.text = p.horarios ?? '';
      _domiciliar = p.recolhimentoDomiciliar;
      _situacao = p.situacao == SituacaoCadastro.inativo ? SituacaoCadastro.inativo : SituacaoCadastro.ativo;
      _endereco.dispose();
      _endereco = CamposEndereco(p.endereco);
    });
  }

  @override
  void dispose() {
    for (final c in [_nome, _contato, _horarios]) {
      c.dispose();
    }
    _endereco.dispose();
    _envio.dispose();
    super.dispose();
  }

  Future<void> _salvar() async {
    if (!_form.currentState!.validate()) return;
    final original = _ponto;
    if (original != null && original.ativo && _situacao == SituacaoCadastro.inativo) {
      final ok = await confirmar(
        context,
        titulo: 'Inativar ponto',
        mensagem:
            '${original.nome} deixará de aparecer no catálogo público e de receber novas solicitações. '
            'A inativação é recusada se houver solicitações abertas.',
        rotuloAcao: 'Inativar ponto',
        destrutiva: true,
      );
      if (!ok) return;
    }
    final salvo = await _envio.executar(
      () => widget.repositorio.salvarPonto(
        id: widget.id,
        nome: _nome.text,
        contato: _contato.text,
        horarios: _horarios.text,
        recolhimentoDomiciliar: _domiciliar,
        situacao: _situacao.api,
        endereco: _endereco.valor(),
      ),
    );
    if (salvo == null || !mounted) return;
    if (widget.id == null) {
      widget.aoCriado(salvo);
    } else {
      _preencher(salvo);
      mostrarMensagem(context, 'Ponto atualizado.');
    }
  }

  @override
  Widget build(BuildContext context) {
    final titulo = widget.id == null ? 'Novo ponto de coleta' : 'Editar ponto de coleta';
    final falhaCarga = _carga.falha;
    if (widget.id != null && _ponto == null) {
      return PaginaRolavel(
        larguraMaxima: Largura.formulario,
        filhos: [
          TituloPagina(titulo),
          if (falhaCarga is ErroHttp && falhaCarga.naoEncontrado)
            EstadoVazio(
              titulo: 'Ponto não encontrado',
              acao: Elevavel(
                child: FilledButton(onPressed: widget.aoVoltar, child: const Text('Voltar aos pontos')),
              ),
            )
          else if (falhaCarga != null)
            EstadoErro(mensagem: falhaCarga.mensagem, aoTentarNovamente: _carregar)
          else
            const EstadoCarregando(),
        ],
      );
    }
    return ListenableBuilder(listenable: _envio, builder: (context, _) => _formulario(context, titulo));
  }

  Widget _formulario(BuildContext context, String titulo) {
    final falha = _envio.falha;
    final conflitoRn05 = falha is ErroHttp && falha.conflitoRegra && widget.id != null;
    return Form(
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
          TituloPagina(titulo),
          const LegendaObrigatorio(),
          MensagemEnvio(
            estado: _envio,
            acao: conflitoRn05
                ? TextButton(
                    onPressed: () => widget.aoVerSolicitacoes(widget.id!),
                    child: const Text('Ver solicitações do ponto'),
                  )
                : null,
          ),
          const TituloSecao('Dados do ponto'),
          CampoTexto(
            controlador: _nome,
            rotulo: 'Nome',
            obrigatorio: true,
            limite: 120,
            erroServidor: _envio.erro('nome'),
          ),
          CampoTexto(
            controlador: _contato,
            rotulo: 'Contato',
            limite: 150,
            ajuda: 'Telefone ou e-mail do ponto.',
            erroServidor: _envio.erro('contato'),
          ),
          CampoTexto(
            controlador: _horarios,
            rotulo: 'Horários de atendimento',
            limite: 500,
            linhas: 2,
            erroServidor: _envio.erro('horarios'),
          ),
          SwitchListTile(
            value: _domiciliar,
            onChanged: (v) => setState(() => _domiciliar = v),
            title: const Text('Faz recolhimento domiciliar'),
            subtitle: const Text('Moradores podem pedir recolhimento em casa.'),
            contentPadding: EdgeInsets.zero,
          ),
          const SizedBox(height: Espaco.s),
          DropdownButtonFormField<SituacaoCadastro>(
            initialValue: _situacao,
            decoration: InputDecoration(labelText: 'Situação *', errorText: _envio.erro('situacao')),
            items: const [
              DropdownMenuItem(value: SituacaoCadastro.ativo, child: Text('Ativo')),
              DropdownMenuItem(value: SituacaoCadastro.inativo, child: Text('Inativo')),
            ],
            onChanged: (v) => setState(() => _situacao = v ?? _situacao),
          ),
          const TituloSecao('Endereço'),
          GrupoEndereco(campos: _endereco, envio: _envio, prefixo: 'endereco.'),
          Wrap(
            alignment: WrapAlignment.end,
            spacing: Espaco.s,
            children: [
              TextButton(onPressed: widget.aoVoltar, child: const Text('Voltar')),
              BotaoEnvio(
                rotulo: widget.id == null ? 'Cadastrar ponto' : 'Salvar ponto',
                enviando: _envio.enviando,
                aoPressionar: _salvar,
              ),
            ],
          ),
          if (_ponto != null)
            SecaoVinculos(
              key: ValueKey('vinculos-${_ponto!.id}'),
              ponto: _ponto!,
              repositorio: widget.repositorio,
              aoAlterar: _carregar,
              aoVerSolicitacoes: () => widget.aoVerSolicitacoes(_ponto!.id),
            )
          else
            const Padding(
              padding: EdgeInsets.only(top: Espaco.l),
              child: Text('Os materiais aceitos podem ser vinculados depois do cadastro do ponto.'),
            ),
        ],
      ),
    );
  }
}

/// Materiais aceitos do ponto: incluir, editar observação e limite, remover.
class SecaoVinculos extends StatefulWidget {
  const SecaoVinculos({
    super.key,
    required this.ponto,
    required this.repositorio,
    required this.aoAlterar,
    required this.aoVerSolicitacoes,
  });

  final PontoColeta ponto;
  final RepositorioCatalogo repositorio;
  final Future<void> Function() aoAlterar;
  final VoidCallback aoVerSolicitacoes;

  @override
  State<SecaoVinculos> createState() => _SecaoVinculosState();
}

class _SecaoVinculosState extends State<SecaoVinculos> {
  String? _aviso;
  TipoAviso _tipo = TipoAviso.info;
  bool _mostrarAtalho = false;

  Future<void> _editar({MaterialAceito? atual}) async {
    MaterialColeta? material;
    if (atual == null) {
      material = await _escolherMaterial();
      if (material == null || !mounted) return;
    }
    final resultado = await abrirFormulario<MaterialAceito>(
      context,
      titulo: atual == null ? 'Vincular ${material!.nome}' : 'Editar ${atual.nomeMaterial}',
      construir: (_) => _FormularioVinculo(
        unidade: atual?.unidade ?? material!.unidade,
        inicial: atual,
        salvar: (obs, limite) => widget.repositorio.salvarVinculo(
          widget.ponto.id,
          atual?.materialId ?? material!.id,
          observacoes: obs,
          limite: limite,
        ),
      ),
    );
    if (resultado == null || !mounted) return;
    setState(() {
      _aviso = atual == null ? 'Material vinculado.' : 'Vínculo atualizado.';
      _tipo = TipoAviso.sucesso;
      _mostrarAtalho = false;
    });
    await widget.aoAlterar();
  }

  Future<void> _remover(MaterialAceito v) async {
    final ok = await confirmar(
      context,
      titulo: 'Remover material',
      mensagem:
          '${v.nomeMaterial} deixará de ser aceito em ${widget.ponto.nome}. '
          'A remoção é recusada se afetar uma solicitação aberta.',
      rotuloAcao: 'Remover material',
      destrutiva: true,
    );
    if (!ok) return;
    try {
      await widget.repositorio.removerVinculo(widget.ponto.id, v.materialId);
      setState(() {
        _aviso = 'Material removido do ponto.';
        _tipo = TipoAviso.sucesso;
        _mostrarAtalho = false;
      });
      await widget.aoAlterar();
    } on FalhaApi catch (e) {
      setState(() {
        _aviso = e.mensagem;
        _tipo = TipoAviso.alerta;
        _mostrarAtalho = e is ErroHttp && e.conflitoRegra;
      });
    }
  }

  Future<MaterialColeta?> _escolherMaterial() {
    final vinculados = {for (final v in widget.ponto.materiaisAceitos) v.materialId};
    return abrirFormulario<MaterialColeta>(
      context,
      titulo: 'Escolher material',
      longo: false,
      construir: (context) => _SeletorMaterialAdmin(repositorio: widget.repositorio, excluir: vinculados),
    );
  }

  @override
  Widget build(BuildContext context) {
    final vinculos = widget.ponto.materiaisAceitos;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        TituloSecao(
          'Materiais aceitos',
          acao: Elevavel(
            child: FilledButton.icon(
              onPressed: () => _editar(),
              icon: const Icon(Icons.add),
              label: const Text('Vincular material'),
            ),
          ),
        ),
        if (_aviso != null) ...[
          Aviso(
            tipo: _tipo,
            mensagem: _aviso!,
            acao: _mostrarAtalho
                ? TextButton(onPressed: widget.aoVerSolicitacoes, child: const Text('Ver solicitações do ponto'))
                : null,
          ),
          const SizedBox(height: Espaco.s),
        ],
        if (vinculos.isEmpty)
          const EstadoVazio(icone: Icons.recycling, titulo: 'Nenhum material vinculado a este ponto')
        else
          for (final v in vinculos)
            Padding(
              key: ValueKey(v.materialId),
              padding: const EdgeInsets.only(bottom: Espaco.s),
              child: Card(
                child: ListTile(
                  contentPadding: const EdgeInsets.symmetric(horizontal: Espaco.m, vertical: Espaco.xs),
                  leading: IconeMaterial(nome: v.nomeMaterial),
                  title: Text(v.nomeMaterial),
                  subtitle: Text(
                    [
                      'Em ${v.unidade.extenso}',
                      v.limite == null ? 'sem limite' : 'limite ${v.limite!.comUnidade(v.unidade)}',
                      if (v.observacoes != null && v.observacoes!.isNotEmpty) v.observacoes!,
                    ].join(' · '),
                  ),
                  trailing: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      IconButton(
                        tooltip: 'Editar vínculo de ${v.nomeMaterial}',
                        icon: const Icon(Icons.edit_outlined),
                        onPressed: () => _editar(atual: v),
                      ),
                      IconButton(
                        tooltip: 'Remover ${v.nomeMaterial} do ponto',
                        icon: const Icon(Icons.link_off),
                        onPressed: () => _remover(v),
                      ),
                    ],
                  ),
                ),
              ),
            ),
      ],
    );
  }
}

class _FormularioVinculo extends StatefulWidget {
  const _FormularioVinculo({required this.unidade, required this.salvar, this.inicial});

  final Unidade unidade;
  final MaterialAceito? inicial;
  final Future<MaterialAceito> Function(String? observacoes, Quantidade? limite) salvar;

  @override
  State<_FormularioVinculo> createState() => _FormularioVinculoState();
}

class _FormularioVinculoState extends State<_FormularioVinculo> {
  final _form = GlobalKey<FormState>();
  late final _obs = TextEditingController(text: widget.inicial?.observacoes);
  late final _limite = TextEditingController(text: widget.inicial?.limite?.texto);
  final _envio = EstadoEnvio();

  @override
  void dispose() {
    _obs.dispose();
    _limite.dispose();
    _envio.dispose();
    super.dispose();
  }

  Future<void> _salvar() async {
    if (!_form.currentState!.validate()) return;
    final texto = _limite.text.trim();
    // O contrato aceita limite fracionário inclusive em peças.
    final limite = texto.isEmpty ? null : LeituraQuantidade.de(texto, unidade: Unidade.kg).valor;
    final r = await _envio.executar(() => widget.salvar(_obs.text, limite));
    if (r != null && mounted) Navigator.of(context).pop(r);
  }

  @override
  Widget build(BuildContext context) {
    return Form(
      key: _form,
      child: SingleChildScrollView(
        padding: const EdgeInsets.all(Espaco.l),
        child: ListenableBuilder(
          listenable: _envio,
          builder: (context, _) => Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              MensagemEnvio(estado: _envio),
              CampoTexto(
                controlador: _obs,
                rotulo: 'Observações',
                limite: 500,
                linhas: 2,
                erroServidor: _envio.erro('observacoes'),
              ),
              CampoTexto(
                controlador: _limite,
                rotulo: 'Limite por solicitação (${widget.unidade.simbolo})',
                ajuda: 'Deixe em branco para não limitar.',
                teclado: const TextInputType.numberWithOptions(decimal: true),
                erroServidor: _envio.erro('limite'),
                validar: (t) => t.isEmpty ? null : LeituraQuantidade.de(t, unidade: Unidade.kg).erro,
              ),
              Wrap(
                alignment: WrapAlignment.end,
                spacing: Espaco.s,
                children: [
                  TextButton(onPressed: () => Navigator.of(context).pop(), child: const Text('Voltar')),
                  BotaoEnvio(rotulo: 'Salvar vínculo', enviando: _envio.enviando, aoPressionar: _salvar),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}

/// Seletor administrativo paginado de materiais, incluindo inativos.
class _SeletorMaterialAdmin extends StatefulWidget {
  const _SeletorMaterialAdmin({required this.repositorio, required this.excluir});

  final RepositorioCatalogo repositorio;
  final Set<int> excluir;

  @override
  State<_SeletorMaterialAdmin> createState() => _SeletorMaterialAdminState();
}

class _SeletorMaterialAdminState extends State<_SeletorMaterialAdmin> {
  final _carga = Carga<Pagina<MaterialColeta>>();

  void _carregar(int pagina) =>
      _carga.executar(() => widget.repositorio.materiais(pagina: pagina, acesso: Acesso.ator), () {
        if (mounted) setState(() {});
      });

  @override
  void initState() {
    super.initState();
    _carregar(0);
  }

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: ConstrainedBox(
        constraints: BoxConstraints(maxHeight: MediaQuery.sizeOf(context).height * 0.8),
        child: ListView(
          shrinkWrap: true,
          children: [
            ConteudoCarga<Pagina<MaterialColeta>>(
              carga: _carga,
              aoTentarNovamente: () => _carregar(0),
              construir: (context, pagina) => Column(
                children: [
                  for (final m in pagina.conteudo)
                    ListTile(
                      enabled: !widget.excluir.contains(m.id),
                      leading: IconeMaterial(nome: m.nome, tamanho: 32),
                      title: Text(m.nome),
                      subtitle: Text(
                        widget.excluir.contains(m.id)
                            ? 'Já vinculado'
                            : '${m.unidade.extenso}${m.ativo ? '' : ' · Inativo'}',
                      ),
                      onTap: () => Navigator.of(context).pop(m),
                    ),
                  Paginador(pagina: pagina, aoMudar: _carregar),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
