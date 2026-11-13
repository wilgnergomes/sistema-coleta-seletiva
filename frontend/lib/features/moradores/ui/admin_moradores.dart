import 'package:material_ui/material_ui.dart';

import '../../../app/tema/tokens.dart';
import '../../../core/api/falhas.dart';
import '../../../core/api/pagina.dart';
import '../../../core/dominio/enums.dart';
import '../../../core/estado/carga.dart';
import '../../../ui/compartilhado/dialogos.dart';
import '../../../ui/compartilhado/estados.dart';
import '../../../ui/compartilhado/layout.dart';
import '../../../ui/compartilhado/paginador.dart';
import '../data/moradores.dart';
import 'enderecos.dart';
import 'formulario_morador.dart';

/// `/administracao/moradores` (RF01): lista paginada.
class PaginaMoradoresAdmin extends StatefulWidget {
  const PaginaMoradoresAdmin({
    super.key,
    required this.repositorio,
    required this.pagina,
    required this.aoMudarPagina,
    required this.aoAbrir,
    required this.aoCadastrar,
  });

  final RepositorioMoradores repositorio;
  final int pagina;
  final ValueChanged<int> aoMudarPagina;
  final Future<void> Function(int id) aoAbrir;
  final VoidCallback aoCadastrar;

  @override
  State<PaginaMoradoresAdmin> createState() => _PaginaMoradoresAdminState();
}

class _PaginaMoradoresAdminState extends State<PaginaMoradoresAdmin> {
  final _carga = Carga<Pagina<Morador>>();

  void _carregar() => _carga.executar(() => widget.repositorio.listar(pagina: widget.pagina), () {
    if (mounted) setState(() {});
  });

  @override
  void initState() {
    super.initState();
    _carregar();
  }

  @override
  void didUpdateWidget(PaginaMoradoresAdmin antigo) {
    super.didUpdateWidget(antigo);
    if (antigo.pagina != widget.pagina) _carregar();
  }

  @override
  Widget build(BuildContext context) {
    return PaginaRolavel(
      larguraMaxima: Largura.densa,
      filhos: [
        TituloPagina(
          'Moradores',
          subtitulo: 'Cadastros, situação e endereços.',
          acao: OutlinedButton.icon(
            onPressed: widget.aoCadastrar,
            icon: const Icon(Icons.person_add_alt),
            label: const Text('Cadastrar morador'),
          ),
        ),
        ConteudoCarga<Pagina<Morador>>(
          carga: _carga,
          aoTentarNovamente: _carregar,
          construir: (context, pagina) => Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text('${pagina.totalElementos} moradores', style: Theme.of(context).textTheme.titleMedium),
              const SizedBox(height: Espaco.s),
              if (pagina.conteudo.isEmpty)
                const EstadoVazio(icone: Icons.groups_outlined, titulo: 'Nenhum morador cadastrado')
              else
                GradeResponsiva(
                  larguraMinima: 300,
                  filhos: [
                    for (final m in pagina.conteudo)
                      Card(
                        key: ValueKey(m.id),
                        child: ListTile(
                          contentPadding: const EdgeInsets.symmetric(horizontal: Espaco.m, vertical: Espaco.xs),
                          title: Text(m.nome),
                          subtitle: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(m.email),
                              if (m.fone != null && m.fone!.isNotEmpty) Text(m.fone!),
                              const SizedBox(height: Espaco.xxs),
                              SeloCadastro(situacao: m.situacao),
                            ],
                          ),
                          trailing: const Icon(Icons.chevron_right),
                          onTap: () async {
                            await widget.aoAbrir(m.id);
                            if (mounted) _carregar();
                          },
                        ),
                      ),
                  ],
                ),
              Paginador(pagina: pagina, desabilitado: _carga.carregando, aoMudar: widget.aoMudarPagina),
            ],
          ),
        ),
      ],
    );
  }
}

/// Edição administrativa do morador (com situação) e seus endereços.
class PaginaMoradorAdmin extends StatefulWidget {
  const PaginaMoradorAdmin({
    super.key,
    required this.id,
    required this.repositorio,
    required this.aoVoltar,
    this.focarEnderecos = false,
  });

  final int id;
  final RepositorioMoradores repositorio;
  final VoidCallback aoVoltar;
  final bool focarEnderecos;

  @override
  State<PaginaMoradorAdmin> createState() => _PaginaMoradorAdminState();
}

class _PaginaMoradorAdminState extends State<PaginaMoradorAdmin> {
  final _carga = Carga<Morador>();
  final _chaveEnderecos = GlobalKey();

  void _carregar() => _carga.executar(() => widget.repositorio.obter(widget.id), () {
    if (!mounted) return;
    setState(() {});
    if (widget.focarEnderecos && _carga.dados != null) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        final c = _chaveEnderecos.currentContext;
        if (c != null) Scrollable.ensureVisible(c);
      });
    }
  });

  @override
  void initState() {
    super.initState();
    _carregar();
  }

  @override
  Widget build(BuildContext context) {
    final falha = _carga.falha;
    return PaginaRolavel(
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
        TituloPagina('Morador #${widget.id}'),
        if (falha is ErroHttp && falha.naoEncontrado)
          EstadoVazio(
            titulo: 'Morador não encontrado',
            acao: FilledButton(onPressed: widget.aoVoltar, child: const Text('Voltar')),
          )
        else
          ConteudoCarga<Morador>(
            carga: _carga,
            aoTentarNovamente: _carregar,
            construir: (context, m) => Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Card(
                  child: Padding(
                    padding: const EdgeInsets.all(Espaco.l),
                    child: FormularioMorador(
                      key: ValueKey('morador-${m.id}-${m.situacao.api}-${m.email}'),
                      inicial: m,
                      permitirSituacao: true,
                      rotuloSalvar: 'Salvar morador',
                      confirmarAntes: (dados) async {
                        if (m.situacao != SituacaoCadastro.ativo || dados.situacao != SituacaoCadastro.inativo) {
                          return true;
                        }
                        return confirmar(
                          context,
                          titulo: 'Inativar morador',
                          mensagem: '${m.nome} não poderá criar novas solicitações enquanto estiver inativo.',
                          rotuloAcao: 'Inativar morador',
                          destrutiva: true,
                        );
                      },
                      salvar: (dados) => widget.repositorio.atualizar(m.id, dados),
                      aoSalvar: (salvo) {
                        _carga.definir(salvo);
                        setState(() {});
                        mostrarMensagem(context, 'Morador atualizado.');
                      },
                    ),
                  ),
                ),
                TituloSecao('Endereços', key: _chaveEnderecos),
                SecaoEnderecos(moradorId: m.id, repositorio: widget.repositorio),
              ],
            ),
          ),
      ],
    );
  }
}
