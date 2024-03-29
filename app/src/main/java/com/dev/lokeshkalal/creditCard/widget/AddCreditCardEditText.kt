package com.dev.lokeshkalal.creditCard.widget


import android.content.Context
import android.graphics.Canvas
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import com.dev.lokeshkalal.creditCard.identifier.CardIdentifierResult
import com.dev.lokeshkalal.creditCard.identifier.service.CardIdentifierService
import com.dev.lokeshkalal.creditCard.validator.service.CardValidatorService
import java.lang.RuntimeException
import android.graphics.Rect
import android.text.Spannable
import android.text.style.BackgroundColorSpan
import com.dev.lokeshkalal.creditCard.R
import com.dev.lokeshkalal.creditCard.addCreditCard.*
import com.dev.lokeshkalal.creditCard.common.Constants
import com.dev.lokeshkalal.creditCard.mask.MaskFormatter
import com.dev.lokeshkalal.creditCard.mask.MaskFormatterImpl
import com.google.android.material.textfield.TextInputEditText


class AddCreditCardEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.editTextStyle
) : TextInputEditText(context, attrs, defStyleAttr) {


    private var cardIdentifierCallback: CardIdentifierCallbacks? = null
    private var cardValidatorCallback: CardValidatorCallbacks? = null
    private var cardIdentifierService: CardIdentifierService? = null
    private var cardValidatorService: CardValidatorService? = null
    private var selfChange = false
    private var currentMaxLength = 0
    private var mask: String
    private var cardNumberBackground: AppCompatTextView? = null
    private val maskFormatter: MaskFormatter


    init {
        maskFormatter = MaskFormatterImpl(getMask(Constants.MAX_DEFAULT_CARD_NUMBER), "X")
        mask = getMask(Constants.MAX_DEFAULT_CARD_NUMBER)
    }

    private val internalTextWatcher = object : TextWatcher {

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

        }

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

        }


        override fun afterTextChanged(text: Editable) {
            if (selfChange) return
            selfChange = true
            applyMask(text)
            selfChange = false
            identifyAndValidateCard(maskFormatter.unformat(text.toString()))
            setBackgroundMask(text)
        }


        private fun identifyAndValidateCard(formattedString: String) {
            val cardIdentifierResult =
                cardIdentifierService?.identifyCard(formattedString) ?: CardIdentifierResult.InvalidCard()

            if (cardIdentifierResult.isValidCard) {
                if (formattedString.length >= cardIdentifierResult.minNumber) {
                    val result = cardValidatorService?.validateCard(formattedString.toString()) ?: false
                    if (result) {
                        cardValidatorCallback?.onValidCard(
                            CreditCard(
                                cardIdentifierResult.cardName,
                                cardIdentifierResult.cardType,
                                formattedString.toString()
                            )
                        )
                    } else {
                        cardValidatorCallback?.onInvalidCard(ValidatorErrorResponseCode.INVALID_CARD)
                    }
                } else {
                    cardValidatorCallback?.onInvalidCard(ValidatorErrorResponseCode.INVALID_LENGTH)
                }
                cardIdentifierCallback?.onCardIdentified(cardIdentifierResult.cardType)
            } else {
                cardIdentifierCallback?.onNoCardIdentified(IdentifierErrorResponseCode.NO_CARD_IDENTIFIED)
                if (formattedString.length >= cardIdentifierResult.minNumber) {
                    cardValidatorCallback?.onInvalidCard(ValidatorErrorResponseCode.INVALID_CARD)
                } else {
                    cardValidatorCallback?.onInvalidCard(ValidatorErrorResponseCode.INVALID_LENGTH)
                }

            }

            setMaskAndLength(cardIdentifierResult.maxNumber)

        }
    }

    private fun setBackgroundMask(text: Editable) {
        text.setSpan(
            BackgroundColorSpan(resources.getColor(R.color.grey_900)),
            0,
            text.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    private fun applyMask(text: Editable) {
        text.apply {
            val editableFilters = filters
            filters = emptyArray()
            val formatted = maskFormatter.format(text.toString())

            val previousLength = length
            val currentLength = formatted.length

            replace(0, previousLength, formatted, 0, currentLength)
            // set correct cursor position when editing
            if (currentLength < previousLength) {
                val currentSelection = findCursorPosition(text, selectionStart)
                setSelection(currentSelection)
            }

            // restore input filters
            filters = editableFilters
        }
    }

    private fun findCursorPosition(text: Editable?, start: Int): Int {
        if (text.isNullOrEmpty()) return start
        val textLength = text.length
        val maskLength = mask.length
        var position = start
        for (i in start until maskLength) {
            if (maskFormatter.isPlaceHolder(mask[i])) {
                break
            }
            position++
        }
        position++
        return if (position < textLength) position else textLength
    }


    init {
        setMaskAndLength(Constants.MAX_DEFAULT_CARD_NUMBER)
        addTextChangedListener(internalTextWatcher)
    }

    fun setCardIdentifierService(cardIdentifierService: CardIdentifierService) {
        this.cardIdentifierService = cardIdentifierService
    }

    fun setCardValidatorService(cardValidatorService: CardValidatorService) {
        this.cardValidatorService = cardValidatorService
    }


    private fun setMaskAndLength(maxNumberAllowed: Int) {
        if (currentMaxLength != maxNumberAllowed) {
            mask = getMask(maxNumberAllowed)
            maskFormatter.setMask(mask,"X")
            cardNumberBackground?.setText(mask)
            setMaxLenth(mask.length)

        }
    }

    private fun setMaxLenth(maxLenth: Int) {
        filters = arrayOf<InputFilter>(InputFilter.LengthFilter(maxLenth))
    }


    private fun getMask(length: Int): String {
        // supporting this for now, can add more
        when (length) {
            16 -> return "XXXX XXXX XXXX XXXX"
            15 -> return "XXXX XXXXXX XXXXX"
            14 -> return "XXXX XXXXXX XXXX"
            else -> return throw RuntimeException("Supports card number range 14-16")
        }

    }

    fun setCardIdentifierCallback(cardIdentifierCallback: CardIdentifierCallbacks) {
        this.cardIdentifierCallback = cardIdentifierCallback
    }

    fun setCardValidatorCallback(cardValidatorCallback: CardValidatorCallbacks) {
        this.cardValidatorCallback = cardValidatorCallback
    }

    fun setBackgroundTextView(card_number_background: AppCompatTextView) {
        this.cardNumberBackground = card_number_background
    }


}



