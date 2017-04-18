RangeSeekbar
============
### A seekbar contains two cursor and support multi-touch.

![Opps! Screen shot has missed](https://github.com/dolphinwang/RangeSeekbar/raw/master/Screenshot.png)

RangeSeekbar have left and right cursors, user can move cursor to make fliter.

How to use
--------------------------------------------------
RangeSeekbar support user to set difference parameters.
				
		1. seekbarHeight:        Height of seekbar.
		2. textSize: 	         Size of text mark.
		3. spaceBetween:         Space between seekbar and text mark.
		4. leftCursorBackground: Background drawable of left cursor, press state supported.
		5. rightCursorBackground:Similar with leftCursorBackground.
		6. markTextArray:        Text of marks. The most important parameter, must be set.
		7. textColorNormal:      Color of text mark in normal state.
		8. textColorSelected:    Color of text mark in selected state.
		9. seekbarColorNormal:   Similar with textColorNormal.
		10.seekbarColorSelected: Similar with textColorSelected.
		11.autoMoveDuration:     Time when a cursor move to a mark index without touch.
		
		Users can also set these in java code.
		
Other supported:

		1. setLeftSelection(int index): Set left cursor to any of text mark(besides the last one).
		2. setRightSelection(int index):Set right cursor to any of text mark(besides the first one).
		3. setOnCursorChangeListener:   Set it to listen when left cursor or right cursor is located on new index.
		
### Developed by:
Roy Wang (dolphinwang@foxmail.com)

If you use this lib. Please let me know.

### License:
Copyright 2014 Roy Wang

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
