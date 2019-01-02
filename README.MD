# CycleMenu [![Awesome](https://cdn.rawgit.com/sindresorhus/awesome/d7305f38d29fed78fa85652e3a63e154dd8e8829/media/badge.svg)](https://github.com/sindresorhus/awesome) <img src="https://www.cleveroad.com/public/comercial/label-android.svg" height="20"> <a href="https://www.cleveroad.com/?utm_source=github&utm_medium=label&utm_campaign=contacts"><img src="https://www.cleveroad.com/public/comercial/label-cleveroad.svg" height="20"></a>
![Header image](/images/header.jpg)

## Meet CycleMenu for Android by Cleveroad

What can be better than an eye-catching design in couple with reasonable features? While using this menu you do not need to worry about the lack of space for some useful features since the scroll type can be endless as well as basic. Choose an appropriate corner, customize your icons and here you go!

![Demo image](/images/demo.gif)


During its development, we have created a custom LayoutManager for the inner RecyclerView in order to display the elements in a circle with the possibility to scroll them.

In case you are in need of beautiful and functional app menu, you are welcome to use CycleMenu. Make your scroll last forever!

[![Awesome](/images/logo-footer.png)](https://www.cleveroad.com/?utm_source=github&utm_medium=label&utm_campaign=contacts)

## Setup and usage
To include this library to your project add dependency in **build.gradle** file:
```groovy
    dependencies {
        implementation 'com.cleveroad:cycle-menu:1.0.2'
    }
```

Use the CycleMenu in layout file
```XML
<com.cleveroad.sy.cyclemenuwidget.CycleMenuWidget
        android:id="@+id/itemCycleMenuWidget"
        android:layout_width="match_parent"
        android:layout_height="match_parent"

        app:cm_autoMaxRadius="220dp"
        app:cm_autoMinRadius="10dp"
        app:cm_corner="right_top"
        app:cm_fixedRadius="200dp"
        app:cm_radius_scale_type="auto"
        app:cm_scroll_type="endless"
        app:cm_item_background_tint="#449955"
        app:cm_corner_image_src="@drawable/ic_corner_image"
        app:cm_ripple_color="#009933"
        />
/>
```
or programmatically
```JAVA
CycleMenuWidget cycleMenuWidget = new CycleMenuWidget(mContext);
cycleMenuWidget.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
container.addView(cycleMenuWidget);
```
To set menu resources from xml resource or Collection use:
```JAVA
cycleMenuWidget.setMenuRes(R.menu.menu_items);
cycleMenuWidget.setMenuItems(Collection<CycleMenuItem> items);
```

You can setup any attribute from xml via method.
Methods for setting radius scaling type:
```JAVA
void setScalingType(RADIUS_SCALING_TYPE scalingType)
void setAutoMinRadius(int autoMinRadius)
void setAutoMaxRadius(int autoMaxRadius)
void setFixedRadius(int fixedRadius)

```
If you choose to use `RADIUS_SCALING_TYPE.AUTO` then you need to setup autoMinRadius and autoMaxRadius. This values will be use to calculate radius of menu.
If menu has small amount of items - the radius will be calculated to fit all items but not less then autoMinRadius. If there is a lot of items in the menu then radius will be equal autoMaxRadius.
if you setup autoMaxRadius bigger then available space for the menu - radius will be reduced.
If you choose to use RADIUS_SCALING_TYPE.FIXED the the menu radius will be equal to fixedRadius. But not bigger then available space for the menu.

To setup scroll type. The scroll type can be `ENDLESS` (infinite scrolling) ans `BASIC` (general scrolling).
```JAVA
void setScrollType(SCROLL scrollType)
```

To setup corner in which will be placed menu use:
```JAVA
void setCorner(CORNER corner)
```
And methods to setup UI:
```JAVA
void setItemsBackgroundTint(ColorStateList itemsBackgroundTint)
void setRippleColor(int rippleColor)
void setCornerImageDrawable(Drawable cornerImageDrawable)
void setCornerImageResource(@DrawableRes int drawableRes)
void setCornerImageBitmap(Bitmap bitmap)
void setCollapsedRadius(int collapsedRadius)
void setBackground(int backgroundColor)
```

For listening changing menu state use
```JAVA
setStateChangeListener(
    new OnStateChangedListener() {
        void onStateChanged(CycleMenuWidget.STATE state){};
        void onOpen(){};
        void onClose(){};
})
```
For listening menu items clicks use
```JAVA
setOnMenuItemClickListener(
    new OnMenuItemClickListener() {
        void onMenuItemClick(View view, int itemPosition) {}
})
```

For providing saving state listener use
```JAVA
setStateSaveListener(
    new StateSaveListener() {
        void saveState(int itemPosition, double lastItemAngleShift){}
});
```

## Changelog
See [changelog history].

## Support

If you have any questions, issues or propositions, please create a <a href="../../issues/new">new issue</a> in this repository.

If you want to hire us, send an email to sales@cleveroad.com or fill the form on <a href="https://www.cleveroad.com/contact">contact page</a>

Follow us:

[![Awesome](/images/social/facebook.png)](https://www.facebook.com/cleveroadinc/)   [![Awesome](/images/social/twitter.png)](https://twitter.com/cleveroadinc)   [![Awesome](/images/social/google.png)](https://plus.google.com/+CleveroadInc)   [![Awesome](/images/social/linkedin.png)](https://www.linkedin.com/company/cleveroad-inc-)   [![Awesome](/images/social/youtube.png)](https://www.youtube.com/channel/UCFNHnq1sEtLiy0YCRHG2Vaw)
<br/>


## License
* * *
    The MIT License (MIT)

    Copyright (c) 2016 Cleveroad Inc.

    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in all
    copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
    SOFTWARE.

[changelog history]: /CHANGELOG.md
