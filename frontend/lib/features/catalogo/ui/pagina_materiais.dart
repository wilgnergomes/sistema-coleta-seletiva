import 'package:material_ui/material_ui.dart';

import '../../../app/tema/tokens.dart';
import '../../../core/api/falhas.dart';
import '../../../core/api/pagina.dart';
import '../../../core/estado/carga.dart';
import '../../../ui/compartilhado/estados.dart';
import '../../../ui/compartilhado/layout.dart';
import '../../../ui/compartilhado/material_visual.dart';
import '../../../ui/compartilhado/paginador.dart';
import '../data/modelos.dart';
import '../data/repositorio_catalogo.dart';

/// Catálogo público de materiais (RF03, RF06).
class PaginaMateriais extends StatefulWidget {
  const PaginaMateriais({
    super.key,
    required this.repositorio,
    required this.pagina,
    required this.aoMudarPagina,
    required this.aoAbrir,
  });

  final RepositorioCatalogo repositorio;
  final int pagina;
  final ValueChanged<int> aoMudarPagina;
  final ValueChanged<int> aoAbrir;

  @override
  State<PaginaMateriais> createState() => _PaginaMateriaisState();
}

class _PaginaMateriaisState extends State<PaginaMateriais> {
  final _carga = Carga<Pagina<MaterialColeta>>();

  void _carregar() => _carga.executar(() => widget.repositorio.materiais(pagina: widget.pagina), () => setState(() {}));

  @override
  void initState() {
    super.initState();
    _carregar();
  }

  @override
  void didUpdateWidget(PaginaMateriais antigo) {
    super.didUpdateWidget(antigo);
    if (antigo.pagina != widget.pagina) _carregar();
  }

  @override
  Widget build(BuildContext context) {
    return PaginaRolavel(
      filhos: [
        const TituloPagina('Materiais', subtitulo: 'Veja como preparar cada material antes da entrega.'),
        ConteudoCarga<Pagina<MaterialColeta>>(
          carga: _carga,
          rotuloCarregando: 'Carregando materiais…',
          aoTentarNovamente: _carregar,
          construir: (context, pagina) {
            if (pagina.conteudo.isEmpty) {
              return const EstadoVazio(icone: Icons.recycling, titulo: 'Nenhum material ativo no catálogo');
            }
            return Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                GradeResponsiva(
                  larguraMinima: 300,
                  filhos: [
                    for (final m in pagina.conteudo)
                      Card(
                        key: ValueKey(m.id),
                        clipBehavior: Clip.antiAlias,
                        child: ListTile(
                          contentPadding: const EdgeInsets.symmetric(horizontal: Espaco.m, vertical: Espaco.xs),
                          leading: IconeMaterial(nome: m.nome),
                          title: Text(m.nome),
                          subtitle: Text(
                            '${m.descricao}\nMedido em ${m.unidade.extenso} (${m.unidade.simbolo})',
                            maxLines: 3,
                            overflow: TextOverflow.ellipsis,
                          ),
                          isThreeLine: true,
                          trailing: const Icon(Icons.chevron_right),
                          onTap: () => widget.aoAbrir(m.id),
                        ),
                      ),
                  ],
                ),
                Paginador(pagina: pagina, aoMudar: widget.aoMudarPagina, desabilitado: _carga.carregando),
              ],
            );
          },
        ),
      ],
    );
  }
}

/// Detalhe do material com orientação de preparo completa.
class PaginaDetalheMaterial extends StatefulWidget {
  const PaginaDetalheMaterial({
    super.key,
    required this.id,
    required this.repositorio,
    required this.aoVoltar,
    required this.aoVerPontos,
  });

  final int id;
  final RepositorioCatalogo repositorio;
  final VoidCallback aoVoltar;
  final ValueChanged<int> aoVerPontos;

  @override
  State<PaginaDetalheMaterial> createState() => _PaginaDetalheMaterialState();
}

class _PaginaDetalheMaterialState extends State<PaginaDetalheMaterial> {
  final _carga = Carga<MaterialColeta>();

  void _carregar() => _carga.executar(() => widget.repositorio.material(widget.id), () => setState(() {}));

  @override
  void initState() {
    super.initState();
    _carregar();
  }

  @override
  Widget build(BuildContext context) {
    final falha = _carga.falha;
    if (falha is ErroHttp && falha.naoEncontrado) {
      return PaginaRolavel(
        larguraMaxima: Largura.formulario,
        filhos: [
          EstadoVazio(
            icone: Icons.recycling,
            titulo: 'Material indisponível',
            mensagem: 'Este material não está ativo no catálogo público.',
            acao: FilledButton(onPressed: widget.aoVoltar, child: const Text('Voltar ao catálogo')),
          ),
        ],
      );
    }
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
        ConteudoCarga<MaterialColeta>(
          carga: _carga,
          aoTentarNovamente: _carregar,
          construir: (context, m) => Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Row(
                children: [
                  IconeMaterial(nome: m.nome, tamanho: 56),
                  const SizedBox(width: Espaco.m),
                  Expanded(
                    child: TituloPagina(m.nome, subtitulo: 'Medido em ${m.unidade.extenso} (${m.unidade.simbolo})'),
                  ),
                ],
              ),
              CampoLeitura(rotulo: 'Descrição', valor: m.descricao),
              const TituloSecao('Como preparar'),
              SelectableText(m.orientacao, style: Theme.of(context).textTheme.bodyLarge),
              const SizedBox(height: Espaco.l),
              Align(
                alignment: Alignment.centerLeft,
                child: FilledButton.icon(
                  onPressed: () => widget.aoVerPontos(m.id),
                  icon: const Icon(Icons.place_outlined),
                  label: const Text('Ver pontos que aceitam'),
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }
}
