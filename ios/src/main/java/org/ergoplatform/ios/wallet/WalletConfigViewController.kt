package org.ergoplatform.ios.wallet

import com.badlogic.gdx.utils.I18NBundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.ergoplatform.ios.ui.*
import org.ergoplatform.persistance.WalletConfig
import org.ergoplatform.uilogic.*
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.foundation.NSArray
import org.robovm.apple.uikit.*

class WalletConfigViewController(val walletId: Int) : ViewControllerWithKeyboardLayoutGuide() {

    private lateinit var addressLabel: UILabel
    private lateinit var nameInputField: UITextField
    private lateinit var nameChangeApplyButton: UIButton
    private lateinit var displaySecretsButton: UIButton
    private var wallet: WalletConfig? = null

    override fun viewDidLoad() {
        super.viewDidLoad()

        val texts = getAppDelegate().texts
        title = texts.get(STRING_TITLE_WALLET_DETAILS)
        view.backgroundColor = UIColor.systemBackground()
        navigationController.navigationBar?.tintColor = UIColor.label()

        addressLabel = Headline2Label()
        addressLabel.isUserInteractionEnabled = true
        addressLabel.textAlignment = NSTextAlignment.Center
        addressLabel.addGestureRecognizer(UITapGestureRecognizer {
            shareText(addressLabel.text, addressLabel)
        })

        val nameInputLabel = Body1Label()
        nameInputLabel.text = texts.get(STRING_LABEL_WALLET_NAME)

        nameInputField = createTextField()
        nameInputField.returnKeyType = UIReturnKeyType.Done
        nameInputField.clearButtonMode = UITextFieldViewMode.Always
        nameInputField.delegate = object : UITextFieldDelegateAdapter() {
            override fun shouldReturn(textField: UITextField?): Boolean {
                doSaveWalletName()
                return true
            }
        }
        nameInputField.addOnEditingChangedListener { nameChangeApplyButton.isEnabled = true }

        val nameChangeApplyButtonContainer = UIView(CGRect.Zero())
        nameChangeApplyButton = TextButton(texts.get(STRING_BUTTON_APPLY))
        nameChangeApplyButtonContainer.addSubview(nameChangeApplyButton)
        nameChangeApplyButton.topToSuperview().bottomToSuperview().rightToSuperview()
        nameChangeApplyButton.addOnTouchUpInsideListener { _, _ -> doSaveWalletName() }

        val deleteButton = UIBarButtonItem(UIBarButtonSystemItem.Trash)
        deleteButton.setOnClickListener {
            onDeleteClicked(texts, deleteButton)
        }
        navigationController.topViewController.navigationItem.rightBarButtonItem = deleteButton

        val descShowSecrets = Body1Label()
        descShowSecrets.text = texts.get(STRING_DESC_DISPLAY_MNEMONIC)
        displaySecretsButton = TextButton(texts.get(STRING_BUTTON_DISPLAY_MNEMONIC))
        displaySecretsButton.addOnTouchUpInsideListener { _, _ -> onDisplaySecretsClicked() }

        val container = UIView()
        val stackView = UIStackView(
            NSArray(
                addressLabel,
                createHorizontalSeparator(),
                nameInputLabel,
                nameInputField,
                nameChangeApplyButtonContainer,
                createHorizontalSeparator(),
                descShowSecrets,
                displaySecretsButton
            )
        )
        stackView.axis = UILayoutConstraintAxis.Vertical
        stackView.spacing = DEFAULT_MARGIN * 3
        stackView.setCustomSpacing(DEFAULT_MARGIN, nameInputLabel)
        stackView.setCustomSpacing(0.0, nameInputField)
        stackView.setCustomSpacing(DEFAULT_MARGIN, nameChangeApplyButton)
        stackView.setCustomSpacing(DEFAULT_MARGIN, descShowSecrets)
        val scrollView = container.wrapInVerticalScrollView()
        container.addSubview(stackView)
        stackView.topToSuperview(topInset = DEFAULT_MARGIN)
            .widthMatchesSuperview(inset = DEFAULT_MARGIN, maxWidth = MAX_WIDTH)
            .bottomToSuperview(bottomInset = DEFAULT_MARGIN)

        view.addSubview(scrollView)
        scrollView.topToSuperview().widthMatchesSuperview().bottomToKeyboard(this)
    }

    override fun viewWillAppear(animated: Boolean) {
        super.viewWillAppear(animated)
        viewControllerScope.launch {
            getAppDelegate().database.loadWalletConfigById(walletId)?.let { walletConfig ->
                wallet = walletConfig

                runOnMainThread {
                    nameInputField.text = walletConfig.displayName
                    addressLabel.text = walletConfig.firstAddress
                    nameChangeApplyButton.isEnabled = false
                    displaySecretsButton.isEnabled = walletConfig.secretStorage != null
                }
            }
        }
    }

    private fun onDeleteClicked(
        texts: I18NBundle,
        deleteButton: UIBarButtonItem
    ) {
        val vc = UIAlertController(
            texts.get(STRING_BUTTON_DELETE),
            texts.get(STRING_LABEL_CONFIRM_DELETE),
            UIAlertControllerStyle.ActionSheet
        )

        val cancel = UIAlertAction(texts.get(STRING_LABEL_CANCEL), UIAlertActionStyle.Cancel) {}
        vc.addAction(cancel)

        val delete = UIAlertAction(texts.get(STRING_BUTTON_DELETE), UIAlertActionStyle.Destructive) {
            doDeleteWallet()
        }
        vc.addAction(delete)
        vc.popoverPresentationController?.barButtonItem = deleteButton

        presentViewController(vc, true) {}
    }

    private fun onDisplaySecretsClicked() {
        wallet?.let {
            startAuthFlow(it) { mnemonic ->
                val texts = getAppDelegate().texts
                val alert =
                    UIAlertController(texts.get(STRING_BUTTON_DISPLAY_MNEMONIC), mnemonic, UIAlertControllerStyle.Alert)
                alert.addAction(UIAlertAction(texts.get(STRING_ZXING_BUTTON_OK), UIAlertActionStyle.Default) {})

                presentViewController(alert, true) {}
            }
        }
    }

    private fun doDeleteWallet() {
        // we use GlobalScope here to not cancel the transaction when we leave this view
        wallet?.let {
            GlobalScope.launch {
                getAppDelegate().database.deleteAllWalletData(it)
            }
        }
        navigationController.popViewController(true)
    }

    private fun doSaveWalletName() {
        nameInputField.text?.let {
            if (it.isNotBlank()) {
                viewControllerScope.launch(Dispatchers.IO) {
                    getAppDelegate().database.updateWalletDisplayName(it, walletId)
                    runOnMainThread { nameChangeApplyButton.isEnabled = false }
                }
            }
        }
    }
}