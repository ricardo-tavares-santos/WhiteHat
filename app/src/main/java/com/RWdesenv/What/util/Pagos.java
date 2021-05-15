package com.RWdesenv.What.util;


public class Pagos {

}


/*
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;

import static com.android.billingclient.api.BillingClient.BillingResponseCode.SERVICE_TIMEOUT;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.OK;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.USER_CANCELED;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.BILLING_UNAVAILABLE;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.ITEM_UNAVAILABLE;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.ERROR;
import static com.android.billingclient.api.BillingClient.SkuType.INAPP;

import java.util.ArrayList;
import java.util.List;


public class Pagos implements PurchasesUpdatedListener, BillingClientStateListener, SkuDetailsResponseListener, ConsumeResponseListener {


    private BillingClient billingClient;
    private Context contextPago;
    private String skuId;
    private List<SkuDetails> misProductos;


    // Constructor de la clase Pagos
    public Pagos(Context context) {

        contextPago = context;

    }


    // Asigna el sku del producto que se quiere comprar
    public void comprar(String skuId) {

        this.skuId = skuId;
        configurarBillingClient();

    }


    // Configura el Billing Client para iniciar la conexión con Google Play Console
    private void configurarBillingClient() {

        //  1. Configura el Billing Client
        billingClient = BillingClient.newBuilder(contextPago)
                .enablePendingPurchases()
                .setListener(this)
                .build();

        // 2. Inicia la conexión y asigna los Listener
        billingClient.startConnection(this);

    }


    @Override
    // Evento salta al llamar billingClient.startConnection()
    public void onBillingSetupFinished(BillingResult billingResult) {

        // Busca compras en el Servidor de Google y las marca como consumidas
        consumeCompras();

        // Verifica que la versión de Play Store sea compatible con INAPP
        if (!billingClient.isReady()) {
            //String mensaje = contextPago.getString(R.string.PAGOS_MENSAJE_VERSIÓN_NO_COMPATIBLE);
            Toast.makeText(contextPago, "version not compatible", Toast.LENGTH_LONG).show();
            return;
        }

        // Verifica que la versión de Play Store sea compatible con Suscripciones
        // if (billingClient.isFeatureSupported(SUBSCRIPTIONS).getResponseCode() != OK) {
        //     String mensaje = contextPago.getString(R.string.PAGOS_MENSAJE_VERSIÓN_NO_COMPATIBLE);
        //     Toast.makeText(contextPago, mensaje, Toast.LENGTH_LONG).show();
        //     return; //GooglePlayNoSoportaComprasDeSuscripciones
        // }

        // Verifica que la Configuración se haya hecho bien, sino muestra mensaje de error
        if (verificaResponseCode(billingResult.getResponseCode()) == OK) {
            consultaProductos();
        }

    }


    // Asigna los elemento que se consultarán a Google y los envía con querySkuDetailsAsync
    private void consultaProductos() {

        // Inicializa constantes
        String ITEM_SKU_1 = "likedirect";
        String ITEM_SKU_2 = "likedirecttop";

        // Agrega los productos que se consultarán a Google
        List<String> skuList = new ArrayList<>();
        skuList.add(ITEM_SKU_1);
        skuList.add(ITEM_SKU_2);
        // TODO Cambiar el ingreso manual de items por una consulta a servidor propio de backend seguro.

        SkuDetailsParams.Builder skuDetailsParams = SkuDetailsParams
                .newBuilder()
                .setSkusList(skuList)
                .setType(BillingClient.SkuType.INAPP);

        // Envía consulta a Google y devuelve el listado de productos mediante onSkuDetailsResponse
        billingClient.querySkuDetailsAsync(skuDetailsParams.build(), this);

    }


    @Override
    // Evento salta cuando Google envía los detalles de los Productos en Venta
    public void onSkuDetailsResponse(BillingResult billingResult, List<SkuDetails> skuDetailsList) {

        if (verificaResponseCode(billingResult.getResponseCode()) == OK) {
            if (skuDetailsList != null) {
                misProductos = skuDetailsList;
                muestraDialogoCompra();
            } else {
                //String mensaje = contextPago.getString(R.string.PAGOS_MENSAJE_NO_SKUDETAILSLIST);
                Toast.makeText(contextPago, "no sku details list", Toast.LENGTH_LONG).show();
            }
        }

    }


    // Lanza el dialogo de compra de Google
    private void muestraDialogoCompra() {

        BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                .setSkuDetails(getSkuIdDetails())
                .build();
        billingClient.launchBillingFlow((Activity) contextPago, flowParams);

    }


    // Obtiene el Producto que se comprará según el Sku ingresado mediante comprar(sku);
    private SkuDetails getSkuIdDetails() {

        if (misProductos == null) return null;
        for (SkuDetails skuProducto : misProductos) {
            if (skuId.equals(skuProducto.getSku())) return skuProducto;
        }
        return null;

    }


    @Override
    // Evento salta cuando se finaliza el Proceso de compra
    public void onPurchasesUpdated(BillingResult billingResult, @Nullable List<Purchase> list) {

        if (verificaResponseCode(billingResult.getResponseCode()) == OK) {
            // Validar compra con consulta a Google para evitar ingeniería inversa de hackers
            if (validaCompra()) {
                // Compra confirmada
                Log.i("Pagos", "Compra encontrada en servidor");
            } else {
                // Compra no encontrada: Mensaje de error - Revocar privilegios
                Log.i("Pagos", "Compra no encontrada posible hacker");
            }
            consumeCompras();
        }

    }


    // Valida la compra y Devuelve True si encuentra la compra del usuario en el Servidor de Google
    private boolean validaCompra() {

        List<Purchase> purchasesList = billingClient.queryPurchases(BillingClient.SkuType.INAPP).getPurchasesList();
        if (purchasesList != null && !purchasesList.isEmpty()) {
            for (Purchase purchase : purchasesList) {
                if (purchase.getSku().equals(skuId)) {
                    return true;
                }
            }
        }
        return false;

    }


    // Busca compras en el Servidor de Google y las marca como consumidas
    private void consumeCompras() {

        Purchase.PurchasesResult queryPurchases = billingClient.queryPurchases(BillingClient.SkuType.INAPP);
        if (queryPurchases.getResponseCode() == OK) {
            List<Purchase> purchasesList = queryPurchases.getPurchasesList();
            if (purchasesList != null && !purchasesList.isEmpty()) {
                for (Purchase purchase : purchasesList) {
                    ConsumeParams params = ConsumeParams.newBuilder()
                            .setPurchaseToken(purchase.getPurchaseToken())
                            .build();
                    billingClient.consumeAsync(params, this);
                }
            }
        }

    }


    @Override
    // Evento salta cuando se ha consumido un producto, Si responseCode = 0, ya se puede volver a comprar
    public void onConsumeResponse(BillingResult billingResult, String purchaseToken) {
        if (billingResult.getResponseCode() == OK) {
            Log.i("Pagos", "Token de Compra: " + purchaseToken + " consumida");
        } else {
            Log.i("Pagos", "Error al consumir compra, responseCode: " + billingResult.getResponseCode());
        }
    }


    @Override
    // Evento salta cuando se pierde la conexión durante una compra
    public void onBillingServiceDisconnected() {
        billingClient.startConnection(this);
    }


    // Verifica que el estado del responseCode sea OK, si no muestra mensaje de Error
    private int verificaResponseCode(int responseCode) {

        if (responseCode == OK) return OK;
        if (responseCode == USER_CANCELED) return USER_CANCELED;

        String mensaje = "";
        switch (responseCode) {
            case SERVICE_TIMEOUT:
                mensaje = "service timeout";
                break;
            case BILLING_UNAVAILABLE:
                mensaje = "billing unavailable";
                break;
            case ITEM_UNAVAILABLE:
                mensaje = "item unavailable";
                break;
            case ERROR:
                mensaje = "error";
                break;
            default:
                mensaje = "error " + " code: " + responseCode;
                break;
        }
        Toast.makeText(contextPago, mensaje, Toast.LENGTH_LONG).show();
        return responseCode;

    }


}
*/