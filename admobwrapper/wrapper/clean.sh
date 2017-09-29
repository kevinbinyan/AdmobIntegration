# Eclipse: Project/Clean/All

cd /Users/assen/perforce/hassenmacher_cygnus_good_dev_gd/dev/gd/msdk &&
( cd platform/ios &&
  rm -rf build AutoWrapSymbols/init-*.dSYM
) &&
( cd platform/android &&
  ( cd sdk &&
    rm -rf build dist
    ( cd fake_app &&
      ndk-build clean &&
      ant clean &&
      rm -rf obj/local libs/armeabi-v7a
    ) &&
    ( cd Playground &&
      ant clean &&
      rm -rf obj/local libs/armeabi-v7a
    ) && 
    ( cd LibraryFuncTest1 &&
      rm -rf obj/local libs/armeabi-v7a bin/* gen/*
    ) 
  ) &&
  ( cd Library &&
    ant clean &&
    ndk-build clean &&
    rm -rf doc/GDN/build apps/samples/*/bin/* apps/samples/*/gen/* InterAppTestFacility/bin/* InterAppTestFacility/gen/* 
  )
) &&
( cd wrap &&
  ( cd wrapper &&
    ant clean &&
    rm -rf resources/android/gd.jar resources/android/gdadd.jar resources/android/wrapper.gd.jar resources/ios/agawlib.zip
    ) &&
  ( cd test/sample/webap &&
    ant clean &&
    rm -rf ./WebContent/WEB-INF/lib/gdwrapper.jar
  ) &&
  ( cd test/android/unittest &&
    ant clean &&
    rm -rf bin/* gen/*
  ) &&
  ( cd test/android/sdklib &&
    ant clean &&
    rm -rf bin/* gen/* libs/armeabi-v7a libs/gd.jar
  ) &&
  ( cd test/android/gdtestapp &&
    ant clean &&
    rm -rf bin/* gen/* build
  ) &&
  ( cd test/android/app1 &&
    rm -rf bin/* gen/*
  )
) &&
rm -rf 3rdparty/gt/doc/android/GDN/build 3rdparty/gt/containercommunication/android/sdk/libs/* &&
cd /Users/assen/perforce/hassenmacher_cygnus_good_dev_gd/dev/gt &&
rm -rf containercommunication/android/Library/bin/* containercommunication/android/Library/gen/* &&
cd ..

# find . -perm +0200 -not -type d -ls


    

