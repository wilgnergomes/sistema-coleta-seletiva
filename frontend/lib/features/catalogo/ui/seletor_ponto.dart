import 'package:material_ui/material_ui.dart';

import '../../../app/tema/tokens.dart';
import '../../../core/api/cliente_api.dart';
import '../../../core/api/falhas.dart';
import '../../../core/api/pagina.dart';
import '../../../ui/compartilhado/estados.dart';
import '../../../ui/compartilhado/paginador.dart';
import '../data/modelos.dart';
import '../data/repositorio_catalogo.dart';

/// Seletor administrativo de ponto: pesquisável por nome, paginado e com
/// pontos inativos identificados.
Future<PontoColeta?> escolherPontoAdmin(BuildContext context, RepositorioCatalogo repositorio, {int? selecionado}) {
  return showModalBottomSheet<PontoColeta>(
    context: context,
    isScrollControlled: true,
    showDragHandle: true,
    builder: (context) => _SeletorPonto(repositorio: repositorio, selecionado: selecionado),
  );
}

class _SeletorPonto extends StatefulWidget {
  const _SeletorPonto({required this.repositorio, this.selecionado});

  final RepositorioCatalogo repositorio;
  final int? selecionado;

  @override
  State<_SeletorPonto> createState() => _SeletorPontoState();
}

class _SeletorPontoState extends State<_SeletorPonto> {
  final _nome = TextEditingController();
  Pagina<PontoColeta>? _pagina;
  FalhaApi? _falha;
  int _geracao = 0;

  @override
  void initState() {
    super.initState();
    _carregar(0);
  }

  @override
  void dispose() {
    _nome.dispose();
    super.dispose();
  }

  Future<void> _carregar(int pagina) async {
    final g = ++_geracao;
    setState(() => _falha = null);
    try {
      final r = await widget.repositorio.pesquisarPontos(
        FiltroPontos(nome: _nome.text.trim().isEmpty ? null : _nome.text.trim(), pagina: pagina),
        acesso: Acesso.ator,
      );
      if (g == _geracao && mounted) setState(() => _pagina = r);
    } on FalhaApi catch (e) {
      if (g == _geracao && mounted) setState(() => _falha = e);
    }
  }

  @override
  Widget build(BuildContext context) {
    final pagina = _pagina;
    return SafeArea(
      child: Padding(
        padding: EdgeInsets.only(bottom: MediaQuery.viewInsetsOf(context).bottom),
        child: ConstrainedBox(
          constraints: BoxConstraints(maxHeight: MediaQuery.sizeOf(context).height * 0.8),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: Espaco.l),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    Semantics(
                      header: true,
                      child: Text('Escolher ponto', style: Theme.of(context).textTheme.titleLarge),
                    ),
                    const SizedBox(height: Espaco.s),
                    TextField(
                      controller: _nome,
                      textInputAction: TextInputAction.search,
                      onSubmitted: (_) => _carregar(0),
                      decoration: InputDecoration(
                        labelText: 'Nome do ponto',
                        suffixIcon: IconButton(
                          tooltip: 'Buscar',
                          icon: const Icon(Icons.search),
                          onPressed: () => _carregar(0),
                        ),
                      ),
                    ),
                  ],
                ),
              ),
              if (_falha != null) EstadoErro(mensagem: _falha!.mensagem, aoTentarNovamente: () => _carregar(0)),
              if (pagina == null && _falha == null) const EstadoCarregando(),
              if (pagina != null)
                Flexible(
                  child: ListView(
                    shrinkWrap: true,
                    children: [
                      if (pagina.conteudo.isEmpty) const EstadoVazio(titulo: 'Nenhum ponto encontrado'),
                      for (final p in pagina.conteudo)
                        ListTile(
                          title: Text(p.nome),
                          subtitle: Text(
                            '${p.endereco.municipio} · ${p.ativo ? 'Ativo' : 'Inativo'}'
                            '${p.recolhimentoDomiciliar ? ' · Recolhimento domiciliar' : ''}',
                          ),
                          selected: p.id == widget.selecionado,
                          trailing: p.id == widget.selecionado ? const Icon(Icons.check) : null,
                          onTap: () => Navigator.of(context).pop(p),
                        ),
                      Paginador(pagina: pagina, aoMudar: _carregar),
                    ],
                  ),
                ),
            ],
          ),
        ),
      ),
    );
  }
}
