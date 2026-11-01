package com.example.labo.model

data class X509CertInfo(
    val subjectDN: String,
    val issuerDN: String,
    val serialNumber: String,
    val notBefore: String,
    val notAfter: String,
    val sha256Fingerprint: String,
    val sha1Fingerprint: String,
    val publicKeyAlgorithm: String,
    val keySize: Int,
    val signatureAlgorithm: String
)

data class AppSignatureReport(
    val apkName: String,
    val apkPath: String,
    val packageName: String,
    val hasV1Scheme: Boolean,
    val hasV2Scheme: Boolean,
    val hasV3Scheme: Boolean,
    val hasV4Scheme: Boolean,
    val certificates: List<X509CertInfo>,
    val signingSchemeDetails: String,
    val securityAssessment: String,
    val sharedUserId: String? = null,
    val issuesFound: List<String> = emptyList()
)

data class KeyMakerConfig(
    val alias: String,
    val commonName: String = "LABO Signer",
    val organization: String = "LABO Project",
    val orgUnit: String = "Engineering",
    val locality: String = "Paris",
    val state: String = "IDF",
    val country: String = "FR",
    val validityYears: Int = 25,
    val algorithm: String = "RSA",
    val keySize: Int = 2048,
    val isCloneMode: Boolean = false,
    val sourceCertDN: String = ""
)

data class KeyProfile(
    val id: String,
    val name: String,
    val alias: String,
    val algorithm: String,
    val keySize: Int,
    val certInfo: X509CertInfo,
    val createdAt: Long = System.currentTimeMillis(),
    val isClone: Boolean = false
)

data class SignatureGroup(
    val name: String,
    val certFingerprint: String,
    val algorithm: String,
    val memberApks: List<String>,
    val sharedUserIds: List<String>
)

data class RomSignChainReport(
    val romName: String,
    val targetPartition: String,
    val totalApksScanned: Int,
    val totalSystemFilesScanned: Int,
    val signatureGroups: List<SignatureGroup>,
    val sharedUidMappings: Map<String, List<String>>,
    val avbTrustStatus: String,
    val vbmetaHashMatch: Boolean,
    val generatedAt: Long = System.currentTimeMillis()
)
