/// Configuração de compilação do aplicativo, lida de `--dart-define`.
///
/// Os valores ficam visíveis no cliente compilado. Nenhuma credencial deve
/// ser passada por aqui (RNF06).
library;

enum ModoApp {
  /// Habilita a entrada de identidade simulada (ADR 0019).
  demo,

  /// Somente a área pública. As rotas restritas ficam indisponíveis.
  publico,
}

class ConfiguracaoApp {
  ConfiguracaoApp({required this.modo, required this.apiBaseUrl, required this.fusoNegocio}) {
    if (apiBaseUrl.trim().isEmpty) {
      throw ArgumentError.value(apiBaseUrl, 'API_BASE_URL', 'não pode ser vazio');
    }
    if (fusoNegocio.trim().isEmpty) {
      throw ArgumentError.value(fusoNegocio, 'BUSINESS_TIME_ZONE', 'não pode ser vazio');
    }
  }

  /// Interpreta os textos recebidos por `--dart-define`.
  factory ConfiguracaoApp.deTextos({required String modo, required String apiBaseUrl, required String fusoNegocio}) {
    final modoApp = switch (modo.trim().toLowerCase()) {
      'demo' => ModoApp.demo,
      'publico' || '' => ModoApp.publico,
      _ => throw ArgumentError.value(modo, 'APP_MODE', 'use "demo" ou "publico"'),
    };
    return ConfiguracaoApp(modo: modoApp, apiBaseUrl: apiBaseUrl.trim(), fusoNegocio: fusoNegocio.trim());
  }

  /// Configuração definida na compilação.
  factory ConfiguracaoApp.doAmbiente() => ConfiguracaoApp.deTextos(
    modo: const String.fromEnvironment('APP_MODE', defaultValue: 'publico'),
    apiBaseUrl: const String.fromEnvironment('API_BASE_URL', defaultValue: '/api'),
    fusoNegocio: const String.fromEnvironment('BUSINESS_TIME_ZONE', defaultValue: 'America/Sao_Paulo'),
  );

  final ModoApp modo;
  final String apiBaseUrl;
  final String fusoNegocio;

  bool get demonstracaoHabilitada => modo == ModoApp.demo;

  /// Resolve a base da API contra a página atual.
  ///
  /// Com `/api`, a Web usa a mesma origem da página, no PC ou no celular
  /// pela rede local. A barra final é removida para evitar `//` nos caminhos.
  Uri resolverBase(Uri paginaAtual) {
    final resolvida = paginaAtual.resolve(apiBaseUrl);
    final caminho = resolvida.path.endsWith('/')
        ? resolvida.path.substring(0, resolvida.path.length - 1)
        : resolvida.path;
    return Uri(
      scheme: resolvida.scheme,
      host: resolvida.host,
      port: resolvida.hasPort ? resolvida.port : null,
      path: caminho,
    );
  }
}
