import 'package:http/http.dart' as http;
import 'package:material_ui/material_ui.dart';

import 'app/conexao.dart';
import 'app/configuracao.dart';
import 'core/api/cliente_api.dart';

void main() {
  final configuracao = ConfiguracaoApp.doAmbiente();
  final base = configuracao.resolverBase(Uri.base);
  final api = ClienteApi(base: base, http: http.Client());
  runApp(AppColetaSeletiva(api: api, base: base));
}

class AppColetaSeletiva extends StatelessWidget {
  const AppColetaSeletiva({super.key, required this.api, required this.base});

  final ClienteApi api;
  final Uri base;

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Coleta Seletiva',
      debugShowCheckedModeBanner: false,
      locale: const Locale('pt', 'BR'),
      supportedLocales: const [Locale('pt', 'BR')],
      localizationsDelegates: GlobalMaterialLocalizations.delegates,
      theme: ThemeData(colorSchemeSeed: const Color(0xFF176B4D)),
      home: PaginaConexao(viewModel: ConexaoViewModel(api), base: base),
    );
  }
}
