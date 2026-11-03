package com.coverit.app.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Bank
import com.adamglin.phosphoricons.regular.Briefcase
import com.adamglin.phosphoricons.regular.CalendarBlank
import com.adamglin.phosphoricons.regular.Car
import com.adamglin.phosphoricons.regular.CreditCard
import com.adamglin.phosphoricons.regular.Drop
import com.adamglin.phosphoricons.regular.EnvelopeSimple
import com.adamglin.phosphoricons.regular.FileCode
import com.adamglin.phosphoricons.regular.FileCsv
import com.adamglin.phosphoricons.regular.FileDoc
import com.adamglin.phosphoricons.regular.FileImage
import com.adamglin.phosphoricons.regular.FileMd
import com.adamglin.phosphoricons.regular.FilePdf
import com.adamglin.phosphoricons.regular.FilePpt
import com.adamglin.phosphoricons.regular.FileTxt
import com.adamglin.phosphoricons.regular.FileXls
import com.adamglin.phosphoricons.regular.FirstAid
import com.adamglin.phosphoricons.regular.FolderSimple
import com.adamglin.phosphoricons.regular.GlobeSimple
import com.adamglin.phosphoricons.regular.GraduationCap
import com.adamglin.phosphoricons.regular.GridFour
import com.adamglin.phosphoricons.regular.Hash
import com.adamglin.phosphoricons.regular.Heart
import com.adamglin.phosphoricons.regular.HouseSimple
import com.adamglin.phosphoricons.regular.IdentificationBadge
import com.adamglin.phosphoricons.regular.IdentificationCard
import com.adamglin.phosphoricons.regular.Info
import com.adamglin.phosphoricons.regular.Lasso
import com.adamglin.phosphoricons.regular.LinkSimple
import com.adamglin.phosphoricons.regular.Mailbox
import com.adamglin.phosphoricons.regular.Phone
import com.adamglin.phosphoricons.regular.QrCode
import com.adamglin.phosphoricons.regular.Rectangle
import com.adamglin.phosphoricons.regular.SmileyBlank
import com.adamglin.phosphoricons.regular.Star
import com.adamglin.phosphoricons.regular.Tag
import com.adamglin.phosphoricons.regular.User
import com.adamglin.phosphoricons.regular.UserCircle
import com.adamglin.phosphoricons.regular.UsersThree
import com.coverit.app.R
import com.coverit.app.data.Library
import com.coverit.app.data.Space
import com.coverit.app.detect.Kind
import com.coverit.app.doc.DocType
import com.coverit.app.redact.Style

val Kind.label: Int
    get() = when (this) {
        Kind.NAME -> R.string.kind_name
        Kind.ID -> R.string.kind_id
        Kind.DNI -> R.string.kind_dni
        Kind.NIE -> R.string.kind_nie
        Kind.NSS -> R.string.kind_nss
        Kind.IBAN -> R.string.kind_iban
        Kind.CARD -> R.string.kind_card
        Kind.EMAIL -> R.string.kind_email
        Kind.PHONE -> R.string.kind_phone
        Kind.IP -> R.string.kind_ip
        Kind.DATE -> R.string.kind_date
        Kind.PLATE -> R.string.kind_plate
        Kind.POSTCODE -> R.string.kind_postcode
        Kind.ADDRESS -> R.string.kind_address
        Kind.PROFILE -> R.string.kind_profile
        Kind.DETAIL -> R.string.kind_detail
        Kind.CODE -> R.string.kind_code
        Kind.FACE -> R.string.kind_face
        Kind.CUSTOM -> R.string.kind_custom
        Kind.MANUAL -> R.string.kind_manual
    }

val Kind.tag: Int
    get() = when (this) {
        Kind.NAME -> R.string.tag_name
        Kind.ID -> R.string.tag_id
        Kind.DNI -> R.string.kind_dni
        Kind.NIE -> R.string.kind_nie
        Kind.NSS -> R.string.tag_nss
        Kind.IBAN -> R.string.kind_iban
        Kind.CARD -> R.string.tag_card
        Kind.EMAIL -> R.string.tag_email
        Kind.PHONE -> R.string.tag_phone
        Kind.IP -> R.string.tag_ip
        Kind.DATE -> R.string.tag_date
        Kind.PLATE -> R.string.tag_plate
        Kind.POSTCODE -> R.string.tag_postcode
        Kind.ADDRESS -> R.string.tag_address
        Kind.PROFILE -> R.string.tag_profile
        Kind.DETAIL -> R.string.tag_detail
        Kind.CODE -> R.string.tag_code
        Kind.FACE -> R.string.tag_face
        Kind.CUSTOM, Kind.MANUAL -> R.string.tag_hidden
    }

val Kind.icon: ImageVector
    get() = when (this) {
        Kind.NAME -> PhosphorIcons.Regular.User
        Kind.ID -> PhosphorIcons.Regular.IdentificationCard
        Kind.DNI -> PhosphorIcons.Regular.IdentificationCard
        Kind.NIE -> PhosphorIcons.Regular.IdentificationBadge
        Kind.NSS -> PhosphorIcons.Regular.FirstAid
        Kind.IBAN -> PhosphorIcons.Regular.Bank
        Kind.CARD -> PhosphorIcons.Regular.CreditCard
        Kind.EMAIL -> PhosphorIcons.Regular.EnvelopeSimple
        Kind.PHONE -> PhosphorIcons.Regular.Phone
        Kind.IP -> PhosphorIcons.Regular.GlobeSimple
        Kind.DATE -> PhosphorIcons.Regular.CalendarBlank
        Kind.PLATE -> PhosphorIcons.Regular.Car
        Kind.POSTCODE -> PhosphorIcons.Regular.Mailbox
        Kind.ADDRESS -> PhosphorIcons.Regular.HouseSimple
        Kind.PROFILE -> PhosphorIcons.Regular.LinkSimple
        Kind.DETAIL -> PhosphorIcons.Regular.Info
        Kind.CODE -> PhosphorIcons.Regular.QrCode
        Kind.FACE -> PhosphorIcons.Regular.SmileyBlank
        Kind.CUSTOM -> PhosphorIcons.Regular.Hash
        Kind.MANUAL -> PhosphorIcons.Regular.Lasso
    }

val Kind.hint: Int
    get() = when (this) {
        Kind.NAME -> R.string.hint_name
        Kind.ID -> R.string.hint_id
        Kind.DNI -> R.string.hint_dni
        Kind.NIE -> R.string.hint_nie
        Kind.NSS -> R.string.hint_nss
        Kind.IBAN -> R.string.hint_iban
        Kind.CARD -> R.string.hint_card
        Kind.EMAIL -> R.string.hint_email
        Kind.PHONE -> R.string.hint_phone
        Kind.IP -> R.string.hint_ip
        Kind.DATE -> R.string.hint_date
        Kind.PLATE -> R.string.hint_plate
        Kind.POSTCODE -> R.string.hint_postcode
        Kind.ADDRESS -> R.string.hint_address
        Kind.PROFILE -> R.string.hint_profile
        Kind.DETAIL -> R.string.hint_detail
        Kind.CODE -> R.string.hint_code
        Kind.FACE -> R.string.hint_face
        Kind.CUSTOM, Kind.MANUAL -> R.string.kind_custom
    }

val Style.label: Int
    get() = when (this) {
        Style.BLACK -> R.string.style_black
        Style.PIXELATE -> R.string.style_pixelate
        Style.BLUR -> R.string.style_blur
        Style.LABEL -> R.string.style_label
    }

val Style.hint: Int
    get() = when (this) {
        Style.BLACK -> R.string.style_black_hint
        Style.PIXELATE -> R.string.style_pixelate_hint
        Style.BLUR -> R.string.style_blur_hint
        Style.LABEL -> R.string.style_label_hint
    }

val Style.icon: ImageVector
    get() = when (this) {
        Style.BLACK -> PhosphorIcons.Regular.Rectangle
        Style.PIXELATE -> PhosphorIcons.Regular.GridFour
        Style.BLUR -> PhosphorIcons.Regular.Drop
        Style.LABEL -> PhosphorIcons.Regular.Tag
    }

@Composable
fun spaceName(library: Library, space: Space): String = when (space) {
    Space.WORK -> stringResource(R.string.space_work)
    Space.PERSONAL -> stringResource(R.string.space_personal)
    Space.STUDY -> stringResource(R.string.space_study)
    else -> library.spaceInfo(space)?.name ?: stringResource(R.string.space_personal)
}

fun spaceIcon(library: Library, space: Space): ImageVector = when (space) {
    Space.WORK -> PhosphorIcons.Regular.Briefcase
    Space.PERSONAL -> PhosphorIcons.Regular.UserCircle
    Space.STUDY -> PhosphorIcons.Regular.GraduationCap
    else -> spaceIconFor(library.spaceInfo(space)?.icon)
}

fun spaceIconFor(key: String?): ImageVector = when (key) {
    "heart" -> PhosphorIcons.Regular.Heart
    "house" -> PhosphorIcons.Regular.HouseSimple
    "bank" -> PhosphorIcons.Regular.Bank
    "health" -> PhosphorIcons.Regular.FirstAid
    "car" -> PhosphorIcons.Regular.Car
    "star" -> PhosphorIcons.Regular.Star
    "people" -> PhosphorIcons.Regular.UsersThree
    else -> PhosphorIcons.Regular.FolderSimple
}

val DocType.icon: ImageVector
    get() = when (this) {
        DocType.PDF -> PhosphorIcons.Regular.FilePdf
        DocType.JPEG, DocType.PNG, DocType.WEBP -> PhosphorIcons.Regular.FileImage
        DocType.DOCX, DocType.ODT -> PhosphorIcons.Regular.FileDoc
        DocType.XLSX, DocType.ODS -> PhosphorIcons.Regular.FileXls
        DocType.PPTX, DocType.ODP -> PhosphorIcons.Regular.FilePpt
        DocType.TXT, DocType.LOG, DocType.EML, DocType.VCF, DocType.ICS -> PhosphorIcons.Regular.FileTxt
        DocType.CSV, DocType.TSV -> PhosphorIcons.Regular.FileCsv
        DocType.JSON, DocType.XML, DocType.HTML -> PhosphorIcons.Regular.FileCode
        DocType.MD -> PhosphorIcons.Regular.FileMd
    }
