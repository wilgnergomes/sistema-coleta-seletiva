import 'package:web/web.dart' as web;

/// Registra `performance.mark(nome)`, lido pelo DevTools ou por
/// `performance.getEntriesByName(nome)`.
void marcarDesempenho(String nome) {
  try {
    web.window.performance.mark(nome);
  } on Object {
    // Medição é auxiliar: nunca interrompe a interface.
  }
}
