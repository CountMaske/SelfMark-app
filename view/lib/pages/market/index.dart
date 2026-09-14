import 'package:flutter/material.dart';

class MarketView extends StatefulWidget {
  MarketView({Key? key}) : super(key: key);

  @override
  _MarketViewState createState() => _MarketViewState();
}

class _MarketViewState extends State<MarketView> {
  @override
  Widget build(BuildContext context) {
    return  Center(
      child: Text('插件市场'),
    );
  }
}