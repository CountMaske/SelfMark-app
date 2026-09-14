import 'package:flutter/material.dart';

class PluginView extends StatefulWidget {
  PluginView({Key? key}) : super(key: key);

  @override
  _PluginViewState createState() => _PluginViewState();
}

class _PluginViewState extends State<PluginView> {
  @override
  Widget build(BuildContext context) {
    return Center(child: Text('我的插件'));
  }
}
