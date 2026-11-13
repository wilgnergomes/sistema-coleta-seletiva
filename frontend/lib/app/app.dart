import 'package:go_router/go_router.dart';
import 'package:material_ui/material_ui.dart';

import 'tema/tema.dart';

class AppColetaSeletiva extends StatelessWidget {
  const AppColetaSeletiva({super.key, required this.rotas});

  final GoRouter rotas;

  @override
  Widget build(BuildContext context) {
    return MaterialApp.router(
      title: 'Coleta Seletiva',
      debugShowCheckedModeBanner: false,
      locale: const Locale('pt', 'BR'),
      supportedLocales: const [Locale('pt', 'BR')],
      localizationsDelegates: GlobalMaterialLocalizations.delegates,
      theme: criarTema(),
      themeMode: ThemeMode.light,
      routerConfig: rotas,
    );
  }
}
