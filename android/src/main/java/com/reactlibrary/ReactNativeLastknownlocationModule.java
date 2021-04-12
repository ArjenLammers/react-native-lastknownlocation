// ReactNativeLastknownlocationModule.java

package com.reactlibrary;

import android.Manifest;
import android.annotation.SuppressLint;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.PromiseImpl;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.permissions.PermissionsModule;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import com.reactnativecommunity.geolocation.PositionError;

public class ReactNativeLastknownlocationModule extends ReactContextBaseJavaModule {

    private final ReactApplicationContext reactContext;
    private FusedLocationProviderClient fusedLocationClient;

    public ReactNativeLastknownlocationModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return "RNLastLocation";
    }

    @ReactMethod
    public void getLastLocation(
            final ReadableMap options,
            final Callback success,
            final Callback error) {


        final PermissionsModule perms = getReactApplicationContext().getNativeModule(
                PermissionsModule.class);
        final Callback onPermissionGranted = new Callback() {
            @Override
            public void invoke(Object... args) {
                String result = (String) args[0];
                if (result == "granted") {
                    getLastLocationData(options, success, error);
                } else {
                    error.invoke(PositionError.buildError(
                            PositionError.PERMISSION_DENIED,
                            "Location permission was not granted."));
                }
            }
        };

        final Callback onPermissionDenied = new Callback() {
            @Override
            public void invoke(Object... args) {
                error.invoke(PositionError.buildError(PositionError.PERMISSION_DENIED,
                        "Failed to request location permission."));
            }
        };

        Callback onPermissionCheckFailed = new Callback() {
            @Override
            public void invoke(Object... args) {
                error.invoke(PositionError.buildError(PositionError.PERMISSION_DENIED,
                        "Failed to check location permission."));
            }
        };

        Callback onPermissionChecked = new Callback() {
            @Override
            public void invoke(Object... args) {
                boolean hasPermission = (boolean) args[0];

                if (!hasPermission) {
                    perms.requestPermission(Manifest.permission.ACCESS_FINE_LOCATION,
                            new PromiseImpl(onPermissionGranted, onPermissionDenied));
                } else {
                    getLastLocationData(options, success, error);
                }
            }
        };

        perms.checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, new PromiseImpl(onPermissionChecked, onPermissionCheckFailed));
    }

    @SuppressLint("MissingPermission")
    public void getLastLocationData(
            ReadableMap options,
            final Callback success,
            Callback error) {
        if (fusedLocationClient == null) {
            this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(
                    this.getReactApplicationContext());
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(
                location -> {
                    if (location != null) {
                        WritableMap result = Arguments.createMap();
                        result.putDouble("accuracy", location.getAccuracy());
                        result.putDouble("latitude", location.getLatitude());
                        result.putDouble("longitude", location.getLongitude());
                        result.putDouble("speed", location.getSpeed());
                        result.putDouble("time", location.getTime());
                        result.putString("provider", location.getProvider());
                        success.invoke(result);
                    } else {
                        success.invoke(Arguments.createMap());
                    }
                }
        ).addOnCanceledListener(() -> {
            error.invoke("canceled");
        }).addOnFailureListener(reason -> {
            error.invoke(reason.getMessage());
        });
    }
}
