package app.olauncher.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.net.Uri
import android.os.UserHandle
import android.provider.ContactsContract
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Filter
import android.widget.Filterable
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import app.olauncher.R
import app.olauncher.data.AppModel
import app.olauncher.data.Constants
import app.olauncher.databinding.AdapterAppDrawerBinding
import app.olauncher.databinding.AdapterCalculatorBinding
import app.olauncher.databinding.AdapterContactBinding
import app.olauncher.databinding.AdapterContactHeaderBinding
import app.olauncher.databinding.AdapterPrivateSpaceHeaderBinding
import app.olauncher.helper.CalculatorHelper
import app.olauncher.helper.FontHelper
import app.olauncher.helper.hideKeyboard
import app.olauncher.helper.isSystemApp
import app.olauncher.helper.showKeyboard
import app.olauncher.helper.showToast
import java.text.Normalizer

class AppDrawerAdapter(
    context: Context,
    private var flag: Int,
    private val appLabelGravity: Int,
    private val fontFamily: String = Constants.FontFamily.LIGHT,
    private val mindfulAppsProvider: () -> Set<String> = { emptySet() },
    private val appClickListener: (AppModel) -> Unit,
    private val appInfoListener: (AppModel) -> Unit,
    private val appDeleteListener: (AppModel) -> Unit,
    private val appHideListener: (AppModel, Int) -> Unit,
    private val appRenameListener: (AppModel, String) -> Unit,
    private val privateSpaceToggleListener: () -> Unit = {},
    private val privateSpaceSettingsListener: () -> Unit = {},
) : ListAdapter<AppModel, RecyclerView.ViewHolder>(DIFF_CALLBACK), Filterable {

    private val appContext = context.applicationContext

    companion object {
        const val VIEW_TYPE_APP = 0
        const val VIEW_TYPE_PRIVATE_HEADER = 1
        const val VIEW_TYPE_CONTACT_HEADER = 2
        const val VIEW_TYPE_CONTACT = 3
        const val VIEW_TYPE_CALCULATOR = 4

        private fun copyCalculatorResult(context: Context, result: AppModel.CalculatorResult) {
            try {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Calculator Result", result.rawResult))
                context.showToast(context.getString(R.string.copied_value, result.formattedResult))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private fun openCalculatorApp(context: Context) {
            try {
                val intent = Intent.makeMainSelectorActivity(
                    Intent.ACTION_MAIN,
                    Intent.CATEGORY_APP_CALCULATOR
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
                context.showToast(context.getString(R.string.unable_to_open_app))
            }
        }

        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<AppModel>() {
            override fun areItemsTheSame(oldItem: AppModel, newItem: AppModel): Boolean = when {
                oldItem is AppModel.App && newItem is AppModel.App ->
                    oldItem.appPackage == newItem.appPackage && oldItem.user == newItem.user

                oldItem is AppModel.PinnedShortcut && newItem is AppModel.PinnedShortcut ->
                    oldItem.shortcutId == newItem.shortcutId && oldItem.user == newItem.user

                oldItem is AppModel.PrivateSpaceHeader && newItem is AppModel.PrivateSpaceHeader -> true

                oldItem is AppModel.Contact && newItem is AppModel.Contact ->
                    oldItem.appLabel == newItem.appLabel && oldItem.phoneNumber == newItem.phoneNumber

                oldItem is AppModel.ContactHeader && newItem is AppModel.ContactHeader -> true

                oldItem is AppModel.CalculatorResult && newItem is AppModel.CalculatorResult -> true

                else -> false
            }

            override fun areContentsTheSame(oldItem: AppModel, newItem: AppModel): Boolean =
                oldItem == newItem
        }
    }

    private var autoLaunch = true
    private var isBangSearch = false
    var allowAutoLaunch = true
    private val diacriticsRegex = Regex("\\p{InCombiningDiacriticalMarks}+")
    private val separatorsRegex = Regex("[-_+,.`'\\s\\p{Z}]")
    private val appFilter = createAppFilter()
    private val myUserHandle = android.os.Process.myUserHandle()

    var appsList: MutableList<AppModel> = mutableListOf()
    var appFilteredList: MutableList<AppModel> = mutableListOf()

    override fun getItemViewType(position: Int): Int {
        return when (currentList.getOrNull(position)) {
            is AppModel.PrivateSpaceHeader -> VIEW_TYPE_PRIVATE_HEADER
            is AppModel.ContactHeader -> VIEW_TYPE_CONTACT_HEADER
            is AppModel.Contact -> VIEW_TYPE_CONTACT
            is AppModel.CalculatorResult -> VIEW_TYPE_CALCULATOR
            else -> VIEW_TYPE_APP
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_PRIVATE_HEADER -> PrivateSpaceHeaderViewHolder(
                AdapterPrivateSpaceHeaderBinding.inflate(inflater, parent, false)
            )

            VIEW_TYPE_CONTACT_HEADER -> ContactHeaderViewHolder(
                AdapterContactHeaderBinding.inflate(inflater, parent, false)
            )

            VIEW_TYPE_CONTACT -> ContactViewHolder(
                AdapterContactBinding.inflate(inflater, parent, false)
            )

            VIEW_TYPE_CALCULATOR -> CalculatorViewHolder(
                AdapterCalculatorBinding.inflate(inflater, parent, false)
            )

            else -> ViewHolder(
                AdapterAppDrawerBinding.inflate(inflater, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        try {
            val pos = holder.bindingAdapterPosition
            if (pos == RecyclerView.NO_POSITION || pos !in currentList.indices) return
            val appModel = currentList[pos]
            when (holder) {
                is PrivateSpaceHeaderViewHolder -> {
                    holder.bind(
                        appLabelGravity,
                        fontFamily,
                        privateSpaceToggleListener,
                        privateSpaceSettingsListener,
                    )
                }

                is ContactHeaderViewHolder -> {
                    holder.bind(appLabelGravity, fontFamily)
                }

                is ContactViewHolder -> {
                    if (appModel is AppModel.Contact) {
                        holder.bind(appModel, appLabelGravity, fontFamily)
                    }
                }

                is CalculatorViewHolder -> {
                    if (appModel is AppModel.CalculatorResult) {
                        holder.bind(appModel, appLabelGravity, fontFamily)
                    }
                }

                is ViewHolder -> {
                    val isMindfulSelected = flag == Constants.FLAG_MINDFUL_APPS &&
                        appModel.appPackage.isNotEmpty() &&
                        mindfulAppsProvider().contains(appModel.appPackage)
                    holder.bind(
                        flag,
                        appLabelGravity,
                        fontFamily,
                        isMindfulSelected,
                        myUserHandle,
                        appModel,
                        appClickListener,
                        appDeleteListener,
                        appInfoListener,
                        appHideListener,
                        appRenameListener
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun getFilter(): Filter = this.appFilter

    private fun createAppFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(charSearch: CharSequence?): FilterResults {
                val queryText = charSearch?.toString().orEmpty()
                isBangSearch = queryText.startsWith("!")
                autoLaunch = allowAutoLaunch &&
                    !queryText.startsWith(" ") &&
                    !CalculatorHelper.isPotentialMathInput(queryText)

                val filtered = (if (charSearch.isNullOrBlank()) appsList.toMutableList()
                else appsList.filter { app ->
                    app !is AppModel.PrivateSpaceHeader && appLabelMatches(app.appLabel, charSearch)
                }.toMutableList())

                if (flag == Constants.FLAG_LAUNCH_APP &&
                    !isBangSearch &&
                    !charSearch.isNullOrBlank()
                ) {
                    CalculatorHelper.evaluate(charSearch.toString())?.let { calc ->
                        filtered.add(
                            0,
                            AppModel.CalculatorResult(
                                expression = calc.expression,
                                formattedResult = calc.formattedResult,
                                rawResult = calc.rawResult
                            )
                        )
                    }
                    if (ContextCompat.checkSelfPermission(
                            appContext,
                            android.Manifest.permission.READ_CONTACTS
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        val contacts = queryContacts(charSearch.toString())
                        if (contacts.isNotEmpty()) {
                            filtered.add(AppModel.ContactHeader())
                            filtered.addAll(contacts)
                        }
                    }
                }

                val filterResults = FilterResults()
                filterResults.values = filtered
                return filterResults
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                results?.values?.let {
                    val items = it as MutableList<AppModel>
                    appFilteredList = items
                    submitList(items.toList()) {
                        autoLaunch()
                    }
                }
            }
        }
    }

    private fun autoLaunch() {
        try {
            if (itemCount == 1
                && autoLaunch
                && isBangSearch.not()
                && flag == Constants.FLAG_LAUNCH_APP
                && appFilteredList.isNotEmpty()
                && appFilteredList[0] !is AppModel.PrivateSpaceHeader
                && appFilteredList[0] !is AppModel.Contact
                && appFilteredList[0] !is AppModel.ContactHeader
                && appFilteredList[0] !is AppModel.CalculatorResult
            ) appClickListener(appFilteredList[0])
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun appLabelMatches(appLabel: String, charSearch: CharSequence): Boolean {
        if (appLabel.contains(charSearch.trim(), true)) return true
        val query = charSearch.normalizeForSearch()
        return query.isNotEmpty() && appLabel.normalizeForSearch().contains(query, true)
    }

    private fun CharSequence.normalizeForSearch(): String =
        Normalizer.normalize(this, Normalizer.Form.NFD)
            .replace(diacriticsRegex, "")
            .replace(separatorsRegex, "")

    fun setAppList(appsList: MutableList<AppModel>) {
        // Add empty app for bottom padding in recyclerview and assign to list
        val newList = appsList.toMutableList()
        newList.add(
            AppModel.App(
                appLabel = "",
                key = null,
                appPackage = "",
                activityClassName = "",
                isNew = false,
                user = android.os.Process.myUserHandle()
            )
        )
        this.appsList = newList
        this.appFilteredList = newList.toMutableList()
        submitList(newList.toList())
    }

    fun removeApp(appModel: AppModel) {
        appsList.remove(appModel)
        appFilteredList = appFilteredList.filter { it != appModel }.toMutableList()
        submitList(appFilteredList.toList())
    }

    fun launchFirstInList() {
        val first = appFilteredList.firstOrNull {
            it !is AppModel.PrivateSpaceHeader &&
            it !is AppModel.Contact &&
            it !is AppModel.ContactHeader
        }
        if (first is AppModel.CalculatorResult) {
            copyCalculatorResult(appContext, first)
            return
        }
        if (first != null) appClickListener(first)
    }

    private fun queryContacts(query: String): List<AppModel.Contact> {
        val contactList = mutableListOf<AppModel.Contact>()
        val uri = Uri.withAppendedPath(
            ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI,
            Uri.encode(query)
        )
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        try {
            appContext.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                if (nameIndex != -1 && numberIndex != -1) {
                    while (cursor.moveToNext()) {
                        val name = cursor.getString(nameIndex) ?: ""
                        val number = cursor.getString(numberIndex) ?: ""
                        if (name.isNotBlank() && number.isNotBlank()) {
                            contactList.add(AppModel.Contact(name, number))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val distinctContacts = contactList.distinctBy {
            "${it.appLabel.lowercase()}|${it.phoneNumber.filter(Char::isDigit).ifEmpty { it.phoneNumber.trim() }}"
        }
        val nameCounts = distinctContacts.groupingBy { it.appLabel.lowercase() }.eachCount()
        return distinctContacts.map {
            it.copy(showNumber = (nameCounts[it.appLabel.lowercase()] ?: 0) > 1)
        }
    }

    class PrivateSpaceHeaderViewHolder(private val binding: AdapterPrivateSpaceHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(
            appLabelGravity: Int,
            fontFamily: String,
            toggleListener: () -> Unit,
            settingsListener: () -> Unit,
        ) = with(binding) {
            privateSpaceTitle.gravity = appLabelGravity
            privateSpaceTitle.typeface = FontHelper.getTypeface(fontFamily)
            privateSpaceTitle.setOnClickListener { toggleListener() }
            privateSpaceTitle.setOnLongClickListener {
                settingsListener()
                true
            }
        }
    }

    class ContactHeaderViewHolder(private val binding: AdapterContactHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(appLabelGravity: Int, fontFamily: String) {
            binding.contactHeaderTitle.gravity = appLabelGravity
            binding.contactHeaderTitle.typeface = FontHelper.getTypeface(fontFamily)
        }
    }

    class CalculatorViewHolder(private val binding: AdapterCalculatorBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(
            result: AppModel.CalculatorResult,
            appLabelGravity: Int,
            fontFamily: String,
        ) = with(binding) {
            calcResult.text = "= ${result.formattedResult}"
            calcResult.gravity = appLabelGravity
            calcHint.gravity = appLabelGravity
            val typeface = FontHelper.getTypeface(fontFamily)
            calcResult.typeface = typeface
            calcHint.typeface = typeface
            root.setOnClickListener { copyCalculatorResult(it.context, result) }
            root.setOnLongClickListener {
                openCalculatorApp(it.context)
                true
            }
        }
    }

    class ContactViewHolder(private val binding: AdapterContactBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(contact: AppModel.Contact, appLabelGravity: Int, fontFamily: String) = with(binding) {
            contactTitle.text = if (contact.showNumber) {
                "📞 ${contact.appLabel} (${contact.phoneNumber})"
            } else {
                "📞 ${contact.appLabel}"
            }
            contactTitle.gravity = appLabelGravity
            contactTitle.typeface = FontHelper.getTypeface(fontFamily)
            contactTitle.setOnClickListener {
                try {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.phoneNumber}")).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    it.context.startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    class ViewHolder(private val binding: AdapterAppDrawerBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var defaultAppName: String = ""

        init {
            binding.etAppRename.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    binding.etAppRename.hint = if (s.isNullOrEmpty()) defaultAppName else ""
                }

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int,
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                }
            })
        }

        fun bind(
            flag: Int,
            appLabelGravity: Int,
            fontFamily: String,
            isMindfulSelected: Boolean,
            myUserHandle: UserHandle,
            appModel: AppModel,
            clickListener: (AppModel) -> Unit,
            appDeleteListener: (AppModel) -> Unit,
            appInfoListener: (AppModel) -> Unit,
            appHideListener: (AppModel, Int) -> Unit,
            appRenameListener: (AppModel, String) -> Unit,
        ) = with(binding) {
            defaultAppName = ""
            appHideLayout.visibility = View.GONE
            renameLayout.visibility = View.GONE
            appTitle.visibility = View.VISIBLE

            // Show indicators in title based on app type and state
            appTitle.text = buildString {
                append(appModel.appLabel)
                when {
                    appModel is AppModel.App && appModel.hasDualClone -> append(" \u00B2")
                    appModel is AppModel.App && appModel.isDualApp -> append(" \u00B7 Dual")
                    appModel.user != myUserHandle -> append(" \u00B7 Work")
                }
                if (appModel.isNew) append(" ✦")
                if (isMindfulSelected) append(" \u23F3")
            }
            appTitle.typeface = FontHelper.getTypeface(fontFamily, bold = isMindfulSelected)
            etAppRename.typeface = FontHelper.getTypeface(fontFamily)
            appTitle.gravity = appLabelGravity
            otherProfileIndicator.isVisible = false

            appTitle.setOnClickListener { clickListener(appModel) }

            appTitle.setOnLongClickListener {
                if (flag != Constants.FLAG_MINDFUL_APPS && appModel.appPackage.isNotEmpty()) {
                    appDelete.alpha = when (
                        appModel is AppModel.PinnedShortcut || !root.context.isSystemApp(appModel.appPackage, appModel.user)
                    ) {
                        true -> 1.0f
                        false -> 0.5f
                    }
                    appHide.text = if (flag == Constants.FLAG_HIDDEN_APPS)
                        root.context.getString(R.string.adapter_show)
                    else
                        root.context.getString(R.string.adapter_hide)
                    appTitle.visibility = View.INVISIBLE
                    appHide.alpha = when (appModel is AppModel.PinnedShortcut) {
                        true -> 0.5f
                        false -> 1.0f
                    }
                    appHideLayout.visibility = View.VISIBLE
                    // Only allow renaming non hidden apps
                    appRename.isVisible = flag != Constants.FLAG_HIDDEN_APPS
                }
                true
            }

            // Configure rename behavior
            appRename.setOnClickListener {
                if (appModel.appPackage.isNotEmpty()) {
                    defaultAppName = getAppName(etAppRename.context, appModel.appPackage, appModel.user)
                    etAppRename.hint = defaultAppName
                    etAppRename.setText(appModel.appLabel)
                    etAppRename.setSelectAllOnFocus(true)
                    renameLayout.visibility = View.VISIBLE
                    appHideLayout.visibility = View.GONE
                    etAppRename.showKeyboard()
                    etAppRename.imeOptions = EditorInfo.IME_ACTION_DONE
                }
            }
            etAppRename.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
                appTitle.visibility = if (hasFocus) View.INVISIBLE else View.VISIBLE
            }
            etAppRename.setOnEditorActionListener { _, actionCode, _ ->
                if (actionCode == EditorInfo.IME_ACTION_DONE) {
                    val renameLabel = etAppRename.text.toString().trim()
                    if (renameLabel.isNotBlank() && appModel.appPackage.isNotBlank()) {
                        appRenameListener(appModel, renameLabel)
                        renameLayout.visibility = View.GONE
                    }
                    true
                }
                false
            }
            tvSaveRename.setOnClickListener {
                etAppRename.hideKeyboard()
                val renameLabel = etAppRename.text.toString().trim()
                if (renameLabel.isNotBlank() && appModel.appPackage.isNotBlank()) {
                    appRenameListener(appModel, renameLabel)
                    renameLayout.visibility = View.GONE
                } else {
                    val fallbackName = defaultAppName.ifBlank {
                        getAppName(etAppRename.context, appModel.appPackage, appModel.user)
                    }
                    appRenameListener(appModel, fallbackName)
                    renameLayout.visibility = View.GONE
                }
            }
            appInfo.setOnClickListener { appInfoListener(appModel) }
            appDelete.setOnClickListener { appDeleteListener(appModel) }
            appMenuClose.setOnClickListener {
                appHideLayout.visibility = View.GONE
                appTitle.visibility = View.VISIBLE
            }
            appRenameClose.setOnClickListener {
                renameLayout.visibility = View.GONE
                appTitle.visibility = View.VISIBLE
            }
            appHide.setOnClickListener { appHideListener(appModel, bindingAdapterPosition) }
        }

        private fun getAppName(context: Context, appPackage: String, user: UserHandle): String {
            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            return try {
                val activityList = launcherApps.getActivityList(appPackage, user)
                if (activityList.isNotEmpty()) {
                    activityList.first().label.toString()
                } else {
                    val packageManager = context.packageManager
                    packageManager.getApplicationLabel(
                        packageManager.getApplicationInfo(appPackage, 0)
                    ).toString()
                }
            } catch (_: Exception) {
                "" // As a fallback, display an empty string.
            }
        }
    }
}
