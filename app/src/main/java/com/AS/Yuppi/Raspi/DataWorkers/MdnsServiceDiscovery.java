package com.AS.Yuppi.Raspi.DataWorkers;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class MdnsServiceDiscovery {
    private static final String TAG = "MdnsServiceDiscovery";
    private static final String SERVICE_TYPE = "_http._tcp.";
    private static final String SERVICE_NAME = "raspiapi"; // Имя сервиса для поиска
    
    private NsdManager nsdManager;
    private NsdManager.DiscoveryListener discoveryListener;
    private String serverUrl;
    private CountDownLatch discoveryLatch;
    private static final long DISCOVERY_TIMEOUT_SECONDS = 10; // Увеличено до 10 секунд
    
    public interface DiscoveryCallback {
        void onServerFound(String url);
        void onDiscoveryFailed();
    }
    
    public MdnsServiceDiscovery(Context context) {
        nsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
    }
    
    public void discoverServer(DiscoveryCallback callback) {
        discoveryLatch = new CountDownLatch(1);
        
        discoveryListener = new NsdManager.DiscoveryListener() {
            @Override
            public void onDiscoveryStarted(String regType) {
                Log.d(TAG, "Service discovery started");
            }
            
            @Override
            public void onServiceFound(NsdServiceInfo service) {
                String serviceName = service.getServiceName();
                String serviceType = service.getServiceType();
                Log.d(TAG, "Service found - Name: " + serviceName + ", Type: " + serviceType);
                
                // Логируем все найденные сервисы для диагностики
                Log.d(TAG, "All service info - Name: " + serviceName + ", Type: " + serviceType + 
                    ", Host: " + (service.getHost() != null ? service.getHost().toString() : "null") +
                    ", Port: " + service.getPort());
                
                // Проверяем, что это наш сервис - проверяем тип и имя
                // Тип может быть с точкой в конце или без (Android иногда добавляет точку)
                boolean isMatchingType = SERVICE_TYPE.equals(serviceType) || 
                    serviceType != null && (serviceType.equals("_http._tcp") || serviceType.startsWith("_http._tcp"));
                // Имя может быть "raspiapi", "raspiapi._http._tcp.local" или содержать "raspi"
                boolean isMatchingName = serviceName != null && 
                    (serviceName.toLowerCase().contains(SERVICE_NAME.toLowerCase()) ||
                     serviceName.toLowerCase().contains("raspi"));
                
                Log.d(TAG, "Service check - Type: " + serviceType + " (match: " + isMatchingType + 
                    "), Name: " + serviceName + " (match: " + isMatchingName + ")");
                
                // Принимаем сервис, если тип совпадает И имя содержит наш идентификатор
                // ИЛИ если тип совпадает и имя содержит "raspi"
                // ВАЖНО: для отладки принимаем ЛЮБОЙ сервис типа _http._tcp, если имя содержит "raspi" или "raspiapi"
                if (isMatchingType) {
                    // Проверяем имя более гибко
                    boolean shouldAccept = false;
                    if (serviceName != null) {
                        String lowerName = serviceName.toLowerCase();
                        shouldAccept = lowerName.contains("raspiapi") || 
                                      lowerName.contains("raspi") ||
                                      lowerName.startsWith("raspiapi");
                    }
                    
                    if (shouldAccept || isMatchingName) {
                        Log.d(TAG, "Found matching service: " + serviceName + ", resolving...");
                        nsdManager.resolveService(service, new NsdManager.ResolveListener() {
                        @Override
                        public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                            Log.e(TAG, "Resolve failed for service: " + serviceInfo.getServiceName() + ", error code: " + errorCode);
                            // Не останавливаем поиск, продолжаем искать другие сервисы
                            // discoveryLatch.countDown() вызываем только при таймауте или успехе
                        }
                        
                        @Override
                        public void onServiceResolved(NsdServiceInfo serviceInfo) {
                            try {
                                String host = serviceInfo.getHost().getHostAddress();
                                int port = serviceInfo.getPort();
                                serverUrl = "http://" + host + ":" + port + "/";
                                Log.d(TAG, "Service successfully resolved: " + serverUrl);
                                Log.d(TAG, "Service name: " + serviceInfo.getServiceName() + ", port: " + port);
                                
                                if (discoveryLatch.getCount() > 0) {
                                    discoveryLatch.countDown();
                                }
                                
                                if (callback != null) {
                                    callback.onServerFound(serverUrl);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error processing resolved service", e);
                            }
                        }
                    });
                    } else {
                        Log.d(TAG, "Service type matches but name doesn't contain 'raspi' - skipping: " + serviceName);
                    }
                } else {
                    Log.d(TAG, "Service type doesn't match - skipping: " + serviceType);
                }
            }
            
            @Override
            public void onServiceLost(NsdServiceInfo service) {
                Log.d(TAG, "Service lost: " + service.getServiceName());
            }
            
            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.d(TAG, "Discovery stopped: " + serviceType);
            }
            
            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed to start: " + errorCode);
                if (discoveryLatch.getCount() > 0) {
                    discoveryLatch.countDown();
                }
                if (callback != null) {
                    callback.onDiscoveryFailed();
                }
            }
            
            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed to stop: " + errorCode);
            }
        };
        
        try {
            Log.d(TAG, "Starting service discovery for type: " + SERVICE_TYPE);
            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
            
            // Ждем результата с таймаутом
            new Thread(() -> {
                try {
                    Log.d(TAG, "Waiting for discovery result (timeout: " + DISCOVERY_TIMEOUT_SECONDS + "s)...");
                    boolean found = discoveryLatch.await(DISCOVERY_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    if (!found) {
                        Log.w(TAG, "Discovery timeout - no service found within " + DISCOVERY_TIMEOUT_SECONDS + " seconds");
                        if (callback != null) {
                            callback.onDiscoveryFailed();
                        }
                    } else {
                        Log.d(TAG, "Discovery completed successfully");
                    }
                } catch (InterruptedException e) {
                    Log.e(TAG, "Discovery wait interrupted", e);
                    if (callback != null) {
                        callback.onDiscoveryFailed();
                    }
                } finally {
                    stopDiscovery();
                }
            }).start();
        } catch (Exception e) {
            Log.e(TAG, "Error starting discovery", e);
            if (callback != null) {
                callback.onDiscoveryFailed();
            }
        }
    }
    
    public String discoverServerSync() {
        Log.d(TAG, "Starting synchronous server discovery...");
        final String[] result = new String[1];
        final CountDownLatch syncLatch = new CountDownLatch(1);
        
        discoverServer(new DiscoveryCallback() {
            @Override
            public void onServerFound(String url) {
                Log.d(TAG, "Server found in sync discovery: " + url);
                result[0] = url;
                syncLatch.countDown();
            }
            
            @Override
            public void onDiscoveryFailed() {
                Log.w(TAG, "Server discovery failed in sync discovery");
                result[0] = null;
                syncLatch.countDown();
            }
        });
        
        try {
            boolean found = syncLatch.await(DISCOVERY_TIMEOUT_SECONDS + 2, TimeUnit.SECONDS);
            if (found && result[0] != null) {
                Log.d(TAG, "Sync discovery completed successfully: " + result[0]);
            } else {
                Log.w(TAG, "Sync discovery completed but no server found");
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "Sync discovery wait interrupted", e);
        }
        
        return result[0];
    }
    
    public void stopDiscovery() {
        if (discoveryListener != null && nsdManager != null) {
            try {
                nsdManager.stopServiceDiscovery(discoveryListener);
            } catch (Exception e) {
                Log.e(TAG, "Error stopping discovery", e);
            }
        }
    }
    
    public String getServerUrl() {
        return serverUrl;
    }
}

