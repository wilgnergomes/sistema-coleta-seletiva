import 'json.dart';

/// Página devolvida pelos endpoints paginados (ADR 0018).
///
/// `pagina` começa em 0. `tamanho` é a capacidade pedida, inclusive na
/// última página.
class Pagina<T> {
  const Pagina({
    required this.conteudo,
    required this.pagina,
    required this.tamanho,
    required this.totalElementos,
    required this.totalPaginas,
  });

  factory Pagina.deJson(Object? json, T Function(Json) item) {
    final mapa = comoMapa(json, 'página');
    return Pagina(
      conteudo: mapa.lista('conteudo', item),
      pagina: mapa.inteiro('pagina'),
      tamanho: mapa.inteiro('tamanho'),
      totalElementos: mapa.inteiro('totalElementos'),
      totalPaginas: mapa.inteiro('totalPaginas'),
    );
  }

  const Pagina.vazia({this.tamanho = 20}) : conteudo = const [], pagina = 0, totalElementos = 0, totalPaginas = 0;

  final List<T> conteudo;
  final int pagina;
  final int tamanho;
  final int totalElementos;
  final int totalPaginas;

  bool get temAnterior => pagina > 0;
  bool get temProxima => pagina + 1 < totalPaginas;

  /// A consulta pediu uma página além da última que ainda tem registros.
  bool get alemDaUltima => conteudo.isEmpty && totalElementos > 0;
}
