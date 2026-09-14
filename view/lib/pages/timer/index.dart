import 'package:flutter/material.dart';

class TimerView extends StatefulWidget {
  TimerView({Key? key}) : super(key: key);

  @override
  _TimerViewState createState() => _TimerViewState();
}

class _TimerViewState extends State<TimerView> {
  @override
  Widget build(BuildContext context) {
    return  Center(
      child: Text('定时器'),
    );
  }
}