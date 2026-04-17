package com.leevaakki.cafe

object PaymentCallbacks {
    var onPaymentSuccess: ((String) -> Unit)? = null
    var onPaymentError: ((Int, String) -> Unit)? = null
}
