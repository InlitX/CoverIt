package com.coverit.app.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Archive
import com.adamglin.phosphoricons.regular.FolderSimple
import com.adamglin.phosphoricons.regular.FolderSimplePlus
import com.adamglin.phosphoricons.regular.PencilSimple
import com.adamglin.phosphoricons.regular.ShieldCheck
import com.adamglin.phosphoricons.regular.Warning
import com.coverit.app.R
import com.coverit.app.doc.Family
import com.coverit.app.doc.Session
import com.coverit.app.ui.MainViewModel
import com.coverit.app.ui.common.CoverSheet
import com.coverit.app.ui.common.NameDialog
import com.coverit.app.ui.common.SpacePicker
import com.coverit.app.ui.components.CoverSwitch
import com.coverit.app.ui.components.IconBadge
import com.coverit.app.ui.components.InputField
import com.coverit.app.ui.components.Pill
import com.coverit.app.ui.components.PrimaryButton
import com.coverit.app.ui.components.SectionLabel
import com.coverit.app.ui.components.SettingRow
import com.coverit.app.ui.components.card

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SaveSheet(vm: MainViewModel, session: Session, onDismiss: () -> Unit) {
    val start = remember { vm.saveOptions(session) }
    var name by remember { mutableStateOf(start.name.substringBeforeLast('.').ifBlank { start.name }) }
    var space by remember { mutableStateOf(start.space) }
    var folder by remember { mutableStateOf(start.folder) }
    var keep by remember { mutableStateOf(start.keepOriginal) }
    var creating by remember { mutableStateOf(false) }
    val chosen = session.chosen
    val pages = chosen.map { it.page }.distinct().size
    val visible = session.marks.count { !it.checked }

    CoverSheet(onDismiss) { close ->
        Text(
            stringResource(if (session.recordId != null) R.string.save_title_changes else R.string.save_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 4.dp),
        )
        Row(Modifier.fillMaxWidth().card().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(if (chosen.isEmpty()) PhosphorIcons.Regular.Warning else PhosphorIcons.Regular.ShieldCheck)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (chosen.isEmpty()) {
                        stringResource(R.string.save_nothing)
                    } else {
                        pluralStringResource(R.plurals.save_covers, chosen.size, chosen.size) + " · " +
                            pluralStringResource(R.plurals.pages_n, pages, pages)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                val detail = when {
                    chosen.isEmpty() -> stringResource(R.string.save_nothing_body)
                    visible > 0 -> pluralStringResource(R.plurals.save_visible, visible, visible)
                    else -> stringResource(
                        if (session.source.type.family == Family.OFFICE) R.string.save_final_office else R.string.save_final,
                    )
                }
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        SectionLabel(stringResource(R.string.docs_name), Modifier.padding(start = 4.dp, top = 4.dp))
        InputField(
            value = name,
            onValueChange = { name = it },
            placeholder = stringResource(R.string.docs_name),
            leading = PhosphorIcons.Regular.PencilSimple,
            capitalization = KeyboardCapitalization.Sentences,
            modifier = Modifier.fillMaxWidth(),
        )

        SectionLabel(stringResource(R.string.docs_space), Modifier.padding(start = 4.dp, top = 4.dp))
        SpacePicker(vm.library, space) {
            space = it
            folder = null
        }
        SectionLabel(stringResource(R.string.docs_folder), Modifier.padding(start = 4.dp, top = 4.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill(stringResource(R.string.docs_no_folder), selected = folder == null) { folder = null }
            vm.library.foldersIn(space).forEach { f -> Pill(f.name, icon = PhosphorIcons.Regular.FolderSimple, selected = folder == f.id) { folder = f.id } }
            Pill(stringResource(R.string.docs_new_folder), icon = PhosphorIcons.Regular.FolderSimplePlus) { creating = true }
        }

        Column(Modifier.fillMaxWidth().padding(top = 4.dp).card()) {
            SettingRow(PhosphorIcons.Regular.Archive, stringResource(R.string.save_keep), stringResource(R.string.save_keep_sub)) {
                CoverSwitch(keep) { keep = it }
            }
        }

        PrimaryButton(
            stringResource(if (session.recordId != null) R.string.editor_save_changes else R.string.save_go),
            Modifier.fillMaxWidth().padding(top = 8.dp),
            PhosphorIcons.Regular.ShieldCheck,
        ) {
            val options = MainViewModel.SaveOptions(name, space, folder, keep)
            close { vm.export(options) }
        }
    }

    if (creating) {
        NameDialog(
            title = stringResource(R.string.docs_new_folder),
            initial = "",
            placeholder = stringResource(R.string.docs_folder_name),
            confirm = stringResource(R.string.add),
            onConfirm = { n -> vm.library.addFolder(n, space)?.also { folder = it.id } != null },
            onDismiss = { creating = false },
        )
    }
}
