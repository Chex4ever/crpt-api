package ru.selsup.crypto;

import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.cms.*;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSTypedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.util.Store;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPrivateKey;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static org.bouncycastle.jce.provider.BouncyCastleProvider.getPrivateKey;

public class CertificateSigningService implements SigningService {
    public static final Logger logger = LoggerFactory.getLogger(CertificateSigningService.class);
    private final CryptoSigner signer;
    private CertificateInfo cert;
    public String defaultAlgo;

    public CertificateSigningService(StorageDiscoverer discoverer) throws NoCertificateFoundException {
        CertificateSelector selector = new ConsoleCertificateSelector();
        List<CertificateInfo> allCerts = discoverer.discoverAllContainers().values().stream().flatMap(List::stream).toList();
        if (allCerts.isEmpty()) {
            throw new NoCertificateFoundException("Не найдено сертификатов");
        }
        this.cert = allCerts.get(0);
        if (allCerts.size() > 1) {
            this.cert = selector.selectCertificate(allCerts);
            if (this.cert == null) {
                throw new NoCertificateFoundException("Не найдено сертификатов");
            }
        }
        logger.info("Сертификат выбран: {}", this.cert.getAlias());
        this.signer = new CryptoSigner();
    }

    @Override
    public CertificateInfo selectedCert() {
        return cert;
    }

    @Override
    public String signData(String data) throws SigningException {
        try {
            if (defaultAlgo != null) {
                return signer.sign(data, cert, defaultAlgo);
            }
            return signer.sign(data, cert);
        } catch (Exception e) {
            throw new SigningException("Failed to sign data", e);
        }
    }

    @Override
    public byte[] signData(byte[] data) throws SigningException {
        try {
            if (defaultAlgo != null) {
                return signer.sign(data, cert, defaultAlgo);
            }
            return signer.sign(data, cert);
        } catch (Exception e) {
            throw new SigningException("Failed to sign data", e);
        }
    }

//    public String signDataAttached(String stringData) throws SigningException {
//        try {
//            byte[] data = stringData.getBytes();
//            KeyStore ks = KeyStore.getInstance(cert.getStoreType(), "JCSP");
//            ks.load(null, null);
//            X509Certificate certificate = (X509Certificate) ks.getCertificate(cert.getAlias());
//            PrivateKey privateKey = (PrivateKey) ks.getKey(cert.getAlias(), null);
//            CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
//
//            ContentSigner contentSigner = new JcaContentSignerBuilder(defaultAlgo).setProvider("JCSP").build(privateKey);
//            SignerInfoGeneratorBuilder signerInfoGenBuilder =
//                    new SignerInfoGeneratorBuilder(new JcaDigestCalculatorProviderBuilder().setProvider("JCSP").build());
//
//            gen.addSignerInfoGenerator(signerInfoGenBuilder.build(contentSigner, new JcaX509CertificateHolder(certificate)));
//            gen.addCertificate(new JcaX509CertificateHolder(certificate));
//
//            CMSSignedData cmsSignedData = gen.generate(new CMSProcessableByteArray(data), true);
//            ByteArrayOutputStream out = new ByteArrayOutputStream();
//            out.write(cmsSignedData.getEncoded());
//
//            byte[] encodedSignature = out.toByteArray();
//            return Base64.getEncoder().encodeToString(encodedSignature);
//
//        } catch (Exception e) {
//
//            throw new SigningException("Ошибка создания присоединенной подписи", e);
//        }
//    }
//
//    public String signDataAttachedv2_old(String stringData) throws SigningException {
//        final Logger logger = LoggerFactory.getLogger(this.getClass());
//
//        try {
//            logger.info("Начало создания присоединенной подписи для данных длиной: {} символов", stringData.length());
//
//            // 1. ПОДГОТОВКА ДАННЫХ
//            byte[] data = stringData.getBytes();
//            logger.debug("Данные преобразованы в байтовый массив длиной: {} байт", data.length);
//
//            // 2. ЗАГРУЗКА КЛЮЧЕВОГО ХРАНИЛИЩА
//            logger.debug("Загрузка KeyStore типа: {} с провайдером: JCSP", cert.getStoreType());
//            KeyStore ks = KeyStore.getInstance(cert.getStoreType(), "JCSP");
//            ks.load(null, null);
//
//            X509Certificate certificate = (X509Certificate) ks.getCertificate(cert.getAlias());
//            PrivateKey privateKey = (PrivateKey) ks.getKey(cert.getAlias(), null);
//
//            if (certificate == null) {
//                logger.error("Сертификат не найден по алиасу: {}", cert.getAlias());
//                throw new SigningException("Сертификат не найден");
//            }
//            if (privateKey == null) {
//                logger.error("Приватный ключ не найден по алиасу: {}", cert.getAlias());
//                throw new SigningException("Приватный ключ не найден");
//            }
//
//            logger.info("Успешно загружен сертификат: {}", certificate.getSubjectDN());
//            logger.debug("Алгоритм приватного ключа: {}", privateKey.getAlgorithm());
//
//            // 3. СОЗДАНИЕ ПОДПИСИ ДАННЫХ
//            logger.debug("Создание подписи с алгоритмом: {}", defaultAlgo);
//            Signature signature = Signature.getInstance(defaultAlgo, "JCSP");
//            signature.initSign(privateKey);
//            signature.update(data);
//            byte[] signatureBytes = signature.sign();
//            logger.info("Raw подпись создана успешно, длина: {} байт", signatureBytes.length);
//
//            // 4. ПОДГОТОВКА CMS ГЕНЕРАТОРА
//            CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
//            gen.addCertificate(new JcaX509CertificateHolder(certificate));
//            logger.debug("Сертификат добавлен в CMS генератор");
//
//            // 5. СОЗДАНИЕ АТРИБУТОВ ПОДПИСИ
//            AlgorithmIdentifier algoId = new DefaultSignatureAlgorithmIdentifierFinder().find(getJcspAlgorithm(defaultAlgo));
//            logger.debug("AlgorithmIdentifier создан: {}", algoId.getAlgorithm());
//
//            ASN1EncodableVector signedAttrs = new ASN1EncodableVector();
//
//            // MessageDigest атрибут
//            MessageDigest digest = MessageDigest.getInstance("GOST3411", "JCSP");
//            byte[] contentDigest = digest.digest(data);
//            logger.debug("Digest данных создан, длина: {} байт", contentDigest.length);
//
//            ASN1EncodableVector digestAttr = new ASN1EncodableVector();
//            digestAttr.add(CMSAttributes.messageDigest);
//            digestAttr.add(new DEROctetString(contentDigest));
//            signedAttrs.add(new DERSet(digestAttr));
//            logger.debug("Атрибут messageDigest добавлен");
//
//            // SigningTime атрибут
//            ASN1EncodableVector signingTimeAttr = new ASN1EncodableVector();
//            signingTimeAttr.add(CMSAttributes.signingTime);
//            signingTimeAttr.add(new Time(new Date()));
//            signedAttrs.add(new DERSet(signingTimeAttr));
//            logger.debug("Атрибут signingTime добавлен");
//
//            // ПРЕОБРАЗОВАНИЕ АТРИБУТОВ
//            ASN1Set signedAttrsSet = new DERSet(signedAttrs);
//            logger.debug("Атрибуты преобразованы в ASN1Set, размер: {}", signedAttrsSet.size());
//
//            // 6. ПОДПИСЬ АТРИБУТОВ
//            logger.debug("Подпись атрибутов...");
//            Signature sigForAttrs = Signature.getInstance(defaultAlgo, "JCSP");
//            sigForAttrs.initSign(privateKey);
//            byte[] encodedAttrs = signedAttrsSet.getEncoded();
//            sigForAttrs.update(encodedAttrs);
//            byte[] signatureOfAttributes = sigForAttrs.sign();
//            logger.info("Атрибуты подписаны успешно, длина подписи: {} байт", signatureOfAttributes.length);
//
//            // 7. СОЗДАНИЕ SIGNERINFO
//            SignerIdentifier signerId = new SignerIdentifier(
//                    new IssuerAndSerialNumber(
//                            new JcaX509CertificateHolder(certificate).getIssuer(),
//                            new JcaX509CertificateHolder(certificate).getSerialNumber()
//                    )
//            );
//            logger.debug("SignerIdentifier создан");
//
//            SignerInfo signerInfo = new SignerInfo(
//                    signerId,
//                    algoId,
//                    signedAttrsSet,
//                    algoId,
//                    new DEROctetString(signatureOfAttributes),
//                    null
//            );
//            logger.debug("SignerInfo создан");
//
//            // 8. СОБИРАЕМ CMS СТРУКТУРУ
//            ASN1Set signerInfos = new DERSet(signerInfo);
//            ASN1Set certificates = new DERSet(new JcaX509CertificateHolder(certificate).toASN1Structure());
//
//            // ВАЖНО: Используем правильный digest algorithm вместо SHA1WithRSA!
//            AlgorithmIdentifier digestAlgoId = new DefaultSignatureAlgorithmIdentifierFinder().find(getJcspAlgorithm(defaultAlgo));
//
//            SignedData signedData = new SignedData(
//                    new DERSet(digestAlgoId), // ← ИСПРАВЛЕНО: используем GOST3411 вместо SHA1
//                    new ContentInfo(CMSObjectIdentifiers.data, new DEROctetString(data)),
//                    certificates,
//                    null,
//                    signerInfos
//            );
//            logger.debug("SignedData создан");
//
//            // 9. ФИНАЛЬНАЯ CMS СТРУКТУРА
//            ContentInfo contentInfo = new ContentInfo(CMSObjectIdentifiers.signedData, signedData);
//            CMSSignedData finalCms = new CMSSignedData(contentInfo);
//
////            ПРОВЕРКА ПОДПИСИ
//            boolean isValid = verifyCMSignature(finalCms);
//            if (!isValid) {
//                logger.error("Самопроверка CMS подписи не пройдена!");
//                throw new SigningException("Созданная подпись не проходит проверку");
//            }
//
//            logger.info("CMS подпись успешно прошла самопроверку");
//
//
//            byte[] encodedCms = finalCms.getEncoded();
//            String base64Result = Base64.getEncoder().encodeToString(encodedCms);
//
//            logger.info("CMS подпись успешно создана, общая длина: {} байт, Base64: {} символов",
//                    encodedCms.length, base64Result.length());
//
//            return base64Result;
//
//        } catch (Exception e) {
//            logger.error("Ошибка создания присоединенной подписи", e);
//            throw new SigningException("Ошибка создания присоединенной подписи", e);
//        }
//    }
//    public String signDataAttachedv2(String stringData) throws SigningException {
//        final Logger logger = LoggerFactory.getLogger(this.getClass());
//        try {
//            findWorkingAlgorithm();
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//        try {
//            logger.info("Начало создания присоединенной подписи");
//
//            byte[] data = stringData.getBytes(StandardCharsets.UTF_8);
//
//            KeyStore ks = KeyStore.getInstance(cert.getStoreType(), "JCSP");
//            ks.load(null, null);
//
//            X509Certificate certificate = (X509Certificate) ks.getCertificate(cert.getAlias());
//            PrivateKey privateKey = (PrivateKey) ks.getKey(cert.getAlias(), null);
//
//            logger.info("Сертификат: {}", certificate.getSubjectDN());
//
//            // 1. СОЗДАЕМ CMS ПОДПИСЬ
//            CMSSignedDataGenerator generator = new CMSSignedDataGenerator();
//
//            // Важно: используем JCSP для подписи, но BC для digest!
//            ContentSigner contentSigner = new JcaContentSignerBuilder("GOST3411withGOST3410")
//                    .setProvider("JCSP") // Подпись через КриптоПро
//                    .build(privateKey);
//
//            // Для digest используем BouncyCastle, а не JCSP!
//            JcaDigestCalculatorProviderBuilder digestBuilder = new JcaDigestCalculatorProviderBuilder()
//                    .setProvider("BC"); // ← ВАЖНО: BC вместо JCSP!
//
//            generator.addSignerInfoGenerator(
//                    new SignerInfoGeneratorBuilder(digestBuilder.build())
//                            .build(contentSigner, new JcaX509CertificateHolder(certificate))
//            );
//
//            generator.addCertificate(new JcaX509CertificateHolder(certificate));
//
//            CMSSignedData signedData = generator.generate(new CMSProcessableByteArray(data), true);
//            logger.info("CMS подпись создана");
//
//            // 2. ПРОВЕРКА ПОДПИСИ С ПРАВИЛЬНЫМИ ПРОВАЙДЕРАМИ
//            boolean isValid = verifyCMSignature(signedData);
//            logger.info("Проверка подписи: {}", isValid);
//
//            if (!isValid) {
//                // Дополнительная диагностика
//                diagnoseSignature(signedData);
//                throw new SigningException("Подпись не прошла проверку");
//            }
//
//            return Base64.getEncoder().encodeToString(signedData.getEncoded());
//
//        } catch (Exception e) {
//            logger.error("Ошибка создания подписи", e);
//            throw new SigningException("Ошибка создания подписи", e);
//        }
//    }
//
//    // Метод проверки с правильными провайдерами
//    private boolean verifyCMSignature(CMSSignedData cmsSignedData) {
//        final Logger logger = LoggerFactory.getLogger(this.getClass());
//
//        try {
//            Store certStore = cmsSignedData.getCertificates();
//            SignerInformationStore signers = cmsSignedData.getSignerInfos();
//
//            for (SignerInformation signer : signers) {
//                Collection<X509CertificateHolder> certCollection = certStore.getMatches(signer.getSID());
//                X509CertificateHolder certHolder = certCollection.iterator().next();
//                X509Certificate certificate = new JcaX509CertificateConverter().getCertificate(certHolder);
//
//                // ВАЖНО: для проверки используем BC провайдер!
//                boolean isValid = signer.verify(new JcaSimpleSignerInfoVerifierBuilder()
//                        .setProvider("BC") // ← Ключевое исправление!
//                        .build(certificate));
//
//                return isValid;
//            }
//            return false;
//
//        } catch (Exception e) {
//            logger.error("Ошибка проверки подписи", e);
//            return false;
//        }
//    }
//
//    // Диагностика подписи
//    private void diagnoseSignature(CMSSignedData cmsSignedData) {
//        final Logger logger = LoggerFactory.getLogger(this.getClass());
//
//        try {
//            SignerInformationStore signers = cmsSignedData.getSignerInfos();
//            Store certStore = cmsSignedData.getCertificates();
//
//            for (SignerInformation signer : signers) {
//                logger.info("Алгоритм подписи: {}", signer.getEncryptionAlgOID());
//                logger.info("Digest алгоритм: {}", signer.getDigestAlgOID());
//                logger.info("Версия SignerInfo: {}", signer.getVersion());
//
//                // Проверяем наличие атрибутов
//                if (signer.getSignedAttributes() != null) {
//                    logger.info("Signed attributes: {}", signer.getSignedAttributes().size());
//                }
//
//                // Пробуем разные провайдеры для проверки
//                Collection<X509CertificateHolder> certCollection = certStore.getMatches(signer.getSID());
//                X509CertificateHolder certHolder = certCollection.iterator().next();
//                X509Certificate certificate = new JcaX509CertificateConverter().getCertificate(certHolder);
//
//                String[] providersToTry = {"BC", "SunPKCS11", "SunRsaSign", "SunJCE"};
//
//                for (String provider : providersToTry) {
//                    try {
//                        boolean valid = signer.verify(new JcaSimpleSignerInfoVerifierBuilder()
//                                .setProvider(provider)
//                                .build(certificate));
//                        logger.info("Проверка с провайдером {}: {}", provider, valid);
//                    } catch (Exception e) {
//                        logger.info("Провайдер {} не сработал: {}", provider, e.getMessage());
//                    }
//                }
//            }
//
//        } catch (Exception e) {
//            logger.error("Ошибка диагностики", e);
//        }
//    }
//    private boolean verifyCMSignature_old(CMSSignedData cmsSignedData) {
//        final Logger logger = LoggerFactory.getLogger(this.getClass());
//
//        try {
//            Store certStore = cmsSignedData.getCertificates();
//            SignerInformationStore signers = cmsSignedData.getSignerInfos();
//
//            logger.debug("Найдено подписей: {}", signers.size());
//
//            for (SignerInformation signer : signers) {
//                logger.debug("Обработка подписи от: {}", signer.getSID());
//
//                // Ищем соответствующий сертификат
//                Collection<X509CertificateHolder> certCollection = certStore.getMatches(signer.getSID());
//                if (certCollection.isEmpty()) {
//                    logger.error("Сертификат для подписи не найден");
//                    return false;
//                }
//
//                X509CertificateHolder certHolder = certCollection.iterator().next();
//                X509Certificate certificate = new JcaX509CertificateConverter().getCertificate(certHolder);
//
//                logger.debug("Найден сертификат: {}", certificate.getSubjectDN());
//
//                // Проверяем подпись
//                boolean isValid = signer.verify(new JcaSimpleSignerInfoVerifierBuilder().build(certificate));
//                logger.debug("Проверка подписи: {}", isValid ? "УСПЕХ" : "ОШИБКА");
//
//                return isValid;
//            }
//
//            return false;
//
//        } catch (Exception e) {
//            logger.error("Ошибка при проверке CMS подписи", e);
//            return false;
//        }
//    }
//    private String getJcspAlgorithm(String defaultAlgo) {
//        switch (defaultAlgo) {
//            case "GOST3411_2012_256withGOST3410_2012_256":
//                return "GOST3411-2012-256withGOST3410-2012-256";
//            case "GOST3411_2012_512withGOST3410_2012_512":
//                return "GOST3411-2012-512withGOST3410-2012-512";
//            case "GOST3411withECGOST3410":
//                return "GOST3411withECGOST3410";
//            default:
//                return "GOST3411withGOST3410";
//        }
//
//    }
//    // Метод для поиска working алгоритма
//    private String findWorkingAlgorithm() throws Exception {
//        final Logger logger = LoggerFactory.getLogger(this.getClass());
//
//        // Алгоритмы, которые понимает BouncyCastle
//        String[] bcAlgorithms = {
//                "GOST3411withGOST3410",
//                "GOST3411withECGOST3410",
//                "SHA256withRSA",  // fallback
//                "SHA1withRSA"     // fallback
//        };
//
//        // Алгоритмы, которые понимает JCSP
//        String[] jcspAlgorithms = {
//                "GOST3411withGOST3410",
//                "GOST3411withECGOST3410",
//                "GOST3411-2012-256withGOST3410-2012-256",
//                "GOST3411-2012-512withGOST3410-2012-512"
//        };
//
//        // Ищем общий алгоритм
//        for (String bcAlgo : bcAlgorithms) {
//            for (String jcspAlgo : jcspAlgorithms) {
//                if (bcAlgo.equalsIgnoreCase(jcspAlgo.replace("-", ""))) {
//                    logger.debug("Найден общий алгоритм: BC={}, JCSP={}", bcAlgo, jcspAlgo);
//
//                    // Проверяем, что JCSP поддерживает этот алгоритм
//                    try {
//                        KeyPairGenerator.getInstance(jcspAlgo, "JCSP");
//                        logger.info("Алгоритм поддерживается: {}", jcspAlgo);
//                        return bcAlgo; // Возвращаем BC-имя
//                    } catch (Exception e) {
//                        logger.debug("Алгоритм не поддерживается JCSP: {}", jcspAlgo);
//                    }
//                }
//            }
//        }
//
//        // Если не нашли, используем самый базовый
//        logger.warn("Общий алгоритм не найден, используем GOST3411withGOST3410");
//        return "GOST3411withGOST3410";
//    }

    public String signDataAttachedv2(String stringData) throws SigningException {
        final Logger logger = LoggerFactory.getLogger(this.getClass());
        logger.info("Начало ручного создания CMS подписи");

        byte[] data = stringData.getBytes(StandardCharsets.UTF_8);
        X509Certificate certificate = null;
        PrivateKey privateKey = null;

        try {
            // 0. Получаем ключи и сертификат
            KeyStore ks = KeyStore.getInstance(cert.getStoreType(), "JCSP");
            ks.load(null, null);
            certificate = (X509Certificate) ks.getCertificate(cert.getAlias());
            privateKey = (PrivateKey) ks.getKey(cert.getAlias(), null);
        } catch (KeyStoreException | NoSuchProviderException | IOException | NoSuchAlgorithmException |
                 CertificateException | UnrecoverableKeyException e) {
            throw new SigningException("Не удалось получить ключи и сертификат");
        }

        logger.debug("Сертификат и ключ загружены");

        // 1. Определяем правильный OID для подписи
        String signatureOid = detectGostOid(privateKey, certificate);
        logger.info("Используется OID алгоритма: {}", signatureOid);

        // 2. Вычисляем digest данных
        byte[] contentDigest = null;
        try {
            MessageDigest digest = MessageDigest.getInstance("GOST3411", "JCSP");
            contentDigest = digest.digest(data);
            logger.debug("Digest вычислен алгоритмом GOST3411 от JCSP : {} байт", contentDigest.length);
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new SigningException("Не удалось Вычисляем digest данных", e);
        }

        // 3. Создаем подписанные атрибуты
        ASN1EncodableVector signedAttrs = new ASN1EncodableVector();

        // Content Type attribute (обязательный)
        signedAttrs.add(new Attribute(
                new ASN1ObjectIdentifier(GostOIDs.CONTENT_TYPE),
                new DERSet(new ASN1ObjectIdentifier(GostOIDs.DATA))
        ));
//        ASN1EncodableVector contentTypeVector = new ASN1EncodableVector();
//        contentTypeVector.add(CMSAttributes.contentType);
//        contentTypeVector.add(CMSObjectIdentifiers.data);
//        signedAttrs.add(new Attribute(CMSAttributes.contentType, new DERSet(CMSObjectIdentifiers.data)));

        // Message Digest attribute (обязательный)
        signedAttrs.add(new Attribute(
                new ASN1ObjectIdentifier(GostOIDs.MESSAGE_DIGEST),
                new DERSet(new DEROctetString(contentDigest))));

        // Signing Time attribute (опциональный, но рекомендуется)
        signedAttrs.add(new Attribute(
                new ASN1ObjectIdentifier(GostOIDs.SIGNING_TIME),
                new DERSet(new DERUTCTime(new Date()))
        ));
//        ASN1EncodableVector signingTimeVector = new ASN1EncodableVector();
//        signingTimeVector.add(CMSAttributes.signingTime);
//        signingTimeVector.add(new Time(new Date()));
//        signedAttrs.add(new Attribute(CMSAttributes.signingTime, new DERSet(new Time(new Date()))));

        DERSet signedAttrsSet = new DERSet(signedAttrs);
        logger.debug("Атрибуты созданы: {} элементов", signedAttrs.size());

        // 4. Подписываем атрибуты через JCSP
        byte[] signatureBytes = null;
        try {
            String jcspAlgorithmName = getJcspAlgorithmName(signatureOid);
            Signature signature = Signature.getInstance(jcspAlgorithmName, "JCSP");
            signature.initSign(privateKey);
            signature.update(signedAttrsSet.getEncoded()); // Подписываем атрибуты, а не данные!
            signatureBytes = signature.sign();
            logger.debug("Атрибуты подписаны алгоритмом {} от JCSP: {} байт", jcspAlgorithmName, signatureBytes.length);
        } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidKeyException | SignatureException |
                 IOException e) {
            throw new SigningException("Не удалось Подписываем атрибуты через JCSP", e);
        }

        // 5. Создаем SignerIdentifier
        X500Name issuer = new X500Name(certificate.getIssuerX500Principal().getName());
        ASN1Integer serial = new ASN1Integer(certificate.getSerialNumber());
        SignerIdentifier signerId = new SignerIdentifier(new IssuerAndSerialNumber(X509Name.getInstance(issuer), serial));

        // 6. Создаем AlgorithmIdentifier для подписи
        AlgorithmIdentifier sigAlgorithm = new AlgorithmIdentifier(
                new ASN1ObjectIdentifier(signatureOid)
        );

        // 7. Создаем AlgorithmIdentifier для digest
        AlgorithmIdentifier digestAlgorithm = new AlgorithmIdentifier(
                new ASN1ObjectIdentifier(signatureOid)
        );

        // 8. Создаем SignerInfo
        SignerInfo signerInfo = new SignerInfo(
                signerId,
                digestAlgorithm,   // digestAlgorithm
                signedAttrsSet,    // signedAttrs
                sigAlgorithm,      // signatureAlgorithm
                new DEROctetString(signatureBytes), // signature
                null               // unsignedAttrs
        );
        logger.debug("SignerInfo создан");

        // 9. Подготавливаем сертификат для CMS
        ASN1Set certificates = null;
        try {
            org.bouncycastle.asn1.x509.Certificate bcCert =
                    org.bouncycastle.asn1.x509.Certificate.getInstance(
                            ASN1Primitive.fromByteArray(certificate.getEncoded())
                    );
            certificates = new DERSet(bcCert);
        } catch (IOException | CertificateEncodingException e) {
            throw new SigningException("Не удалось Подготавливаем сертификат для CMS");
        }

        // 10. Создаем SignedData
        ASN1Set digestAlgorithms = new DERSet(digestAlgorithm);
        ASN1Set signerInfos = new DERSet(signerInfo);

        SignedData signedData = new SignedData(
                digestAlgorithms,                          // digestAlgorithms
                new ContentInfo(CMSObjectIdentifiers.data, // encapsulatedContentInfo
                        new DEROctetString(data)),
                certificates,                              // certificates
                null,                                      // crls
                signerInfos                                // signerInfos
        );
        logger.debug("SignedData создан");

        // 11. Создаем ContentInfo
        ContentInfo contentInfo = new ContentInfo(
                CMSObjectIdentifiers.signedData,
                signedData
        );

        // 12. Кодируем в DER
        byte[] encodedCMS = null;
        try {
            encodedCMS = contentInfo.getEncoded();
        } catch (IOException e) {
            throw new SigningException("Не удалось Кодируем в DER");
        }
        String base64Result = Base64.getEncoder().encodeToString(encodedCMS);

        logger.info("CMS подпись создана успешно: {} байт", encodedCMS.length);


        try {
            verifySignatureWithJCSP(base64Result);
//            diagnoseSignature(base64Result);
//            verifySignatureManually(base64Result);
        } catch (Exception e) {
            throw new SigningException("Проверка не пройдена",e);
        }

        return base64Result;
    }
    public static class GostOIDs {
        // ГОСТ Р 34.10-2012 с ключом 256 бит
        public static final String GOST3411_2012_256_WITH_GOST3410_2012_256 = "1.2.643.7.1.1.3.2";

        // ГОСТ Р 34.10-2012 с ключом 512 бит
        public static final String GOST3411_2012_512_WITH_GOST3410_2012_512 = "1.2.643.7.1.1.3.3";

        // ГОСТ Р 34.10-2001 (старый)
        public static final String GOST3411_WITH_GOST3410 = "1.2.643.2.2.3";
        public static final String GOST3411_WITH_ECGOST3410 = "1.2.643.2.2.4";

        // OID для атрибутов CMS
        public static final String CONTENT_TYPE = "1.2.840.113549.1.9.3";
        public static final String MESSAGE_DIGEST = "1.2.840.113549.1.9.4";
        public static final String SIGNING_TIME = "1.2.840.113549.1.9.5";
        public static final String DATA = "1.2.840.113549.1.7.1";
    }
    private String detectGostOid(PrivateKey privateKey, X509Certificate certificate) {
        // Определяем OID based on размера ключа и алгоритма
        int keySize = getKeySize(privateKey);
        String algorithm = privateKey.getAlgorithm();

        if (algorithm.contains("2012")) {
            return keySize <= 256 ?
                    GostOIDs.GOST3411_2012_256_WITH_GOST3410_2012_256 :
                    GostOIDs.GOST3411_2012_512_WITH_GOST3410_2012_512;
        } else {
            return algorithm.contains("EC") ?
                    GostOIDs.GOST3411_WITH_ECGOST3410 :
                    GostOIDs.GOST3411_WITH_GOST3410;
        }
    }
    public class JcspAlgorithms {
        public static final String GOST3411_2012_256_WITH_GOST3410_2012_256 = "GOST3411-2012-256withGOST3410-2012-256";
        public static final String GOST3411_2012_512_WITH_GOST3410_2012_512 = "GOST3411-2012-512withGOST3410-2012-512";
        // ЕДИНСТВЕННЫЙ алгоритм хеширования в JCSP
        public static final String GOST3411 = "GOST3411"; // Автоматически выбирает размер based on контекста
        public boolean verifySignatureManually(String cmsBase64) throws Exception {
            byte[] cmsData = Base64.getDecoder().decode(cmsBase64);
            CMSSignedData cmsSignedData = new CMSSignedData(cmsData);

            SignerInformationStore signers = cmsSignedData.getSignerInfos();
            Store certStore = cmsSignedData.getCertificates();

            for (SignerInformation signer : signers) {
                // Получаем сертификат
                Collection<X509CertificateHolder> certCollection = certStore.getMatches(signer.getSID());
                X509CertificateHolder certHolder = certCollection.iterator().next();
                X509Certificate certificate = new JcaX509CertificateConverter().getCertificate(certHolder);

                // Получаем подписанные данные
                byte[] signedData = (byte[]) cmsSignedData.getSignedContent().getContent();

                // Получаем подпись
                byte[] signatureBytes = signer.getSignature();

                // Проверяем через JCSP
                Signature verifier = Signature.getInstance(getJcspAlgorithmName(GostOIDs.GOST3411_2012_256_WITH_GOST3410_2012_256), "JCSP");
                verifier.initVerify(certificate);
                verifier.update(signedData);

                return verifier.verify(signatureBytes);
            }

            return false;
        }

    }

    private int getKeySize(PrivateKey privateKey) {
        try {
            if (privateKey instanceof ECPrivateKey) {
                return ((ECPrivateKey) privateKey).getParams().getOrder().bitLength();
            }
            // Другие типы ключей...
        } catch (Exception e) {
            return 256; // default
        }
        return 256;
    }

    private String getJcspAlgorithmName(String oid) {
        return switch (oid) {
            case GostOIDs.GOST3411_2012_256_WITH_GOST3410_2012_256 -> "GOST3411_2012_256withGOST3410_2012_256";
            case GostOIDs.GOST3411_2012_512_WITH_GOST3410_2012_512 -> "GOST3411_2012_512withGOST3410_2012_512";
            case GostOIDs.GOST3411_WITH_ECGOST3410 -> "GOST3411withECGOST3410";
            default -> "GOST3411";
        };
    }
//    public boolean verifyCMSignature(String cmsBase64) throws Exception {
//        final Logger logger = LoggerFactory.getLogger(this.getClass());
//
//        try {
//            byte[] cmsData = Base64.getDecoder().decode(cmsBase64);
//            CMSSignedData cmsSignedData = new CMSSignedData(cmsData);
//
//            Store certStore = cmsSignedData.getCertificates();
//            SignerInformationStore signers = cmsSignedData.getSignerInfos();
//
//            logger.info("Найдено подписей: {}", signers.size());
//
//            for (SignerInformation signer : signers) {
//                logger.info("Обработка подписи от: {}", signer.getSID().getSerialNumber());
//
//                // ПРАВИЛЬНОЕ получение сертификата
//                Collection<X509CertificateHolder> certCollection = certStore.getMatches(signer.getSID());
//                if (certCollection.isEmpty()) {
//                    logger.error("Сертификат не найден для подписи");
//                    return false;
//                }
//
//                X509CertificateHolder certHolder = certCollection.iterator().next();
//                X509Certificate certificate = new JcaX509CertificateConverter()
//                        .setProvider("BC") // Важно указать провайдер
//                        .getCertificate(certHolder);
//
//                logger.info("Сертификат найден: {}", certificate.getSubjectDN());
//
//                // Проверяем подпись
//                try {
//                    boolean isValid = signer.verify(new JcaSimpleSignerInfoVerifierBuilder()
//                            .setProvider("BC")
//                            .build(certificate));
//
//                    logger.info("Проверка подписи: {}", isValid ? "УСПЕХ" : "ОШИБКА");
//                    return isValid;
//
//                } catch (Exception e) {
//                    logger.error("Ошибка проверки подписи: {}", e.getMessage());
//                    return false;
//                }
//            }
//
//            logger.error("Не найдено подписей для проверки");
//            return false;
//
//        } catch (Exception e) {
//            logger.error("Ошибка разбора CMS: {}", e.getMessage());
//            return false;
//        }
//    }
//
//    public void diagnoseSignature(String cmsBase64) throws Exception {
//        final Logger logger = LoggerFactory.getLogger(this.getClass());
//
//        byte[] cmsData = Base64.getDecoder().decode(cmsBase64);
//        CMSSignedData cmsSignedData = new CMSSignedData(cmsData);
//
//        // 1. Информация о CMS
//        logger.info("CMS Version: {}", cmsSignedData.getVersion());
//        logger.info("Encapsulated: {}", cmsSignedData.isDetachedSignature());
//
//        // 2. Информация о сертификатах
//        Store certStore = cmsSignedData.getCertificates();
//        Collection<X509CertificateHolder> certs = certStore.getMatches(null);
//        logger.info("Найдено сертификатов: {}", certs.size());
//
//        for (X509CertificateHolder certHolder : certs) {
//            X509Certificate cert = new JcaX509CertificateConverter().getCertificate(certHolder);
//            logger.info("Сертификат: {}", cert.getSubjectDN());
//            logger.info("  Серийный номер: {}", cert.getSerialNumber());
//            logger.info("  Алгоритм: {}", cert.getSigAlgName());
//        }
//
//        // 3. Информация о подписях
//        SignerInformationStore signers = cmsSignedData.getSignerInfos();
//        logger.info("Найдено подписей: {}", signers.size());
//
//        for (SignerInformation signer : signers) {
//            logger.info("Подпись:");
//            logger.info("  SID: {}", signer.getSID());
//            logger.info("  Алгоритм подписи: {}", signer.getEncryptionAlgOID());
//            logger.info("  Алгоритм digest: {}", signer.getDigestAlgOID());
//            logger.info("  Подписанные атрибуты: {}",
//                    signer.getSignedAttributes() != null ? signer.getSignedAttributes().size() : 0);
//
//            // Пробуем найти соответствующий сертификат
//            Collection<X509CertificateHolder> matchingCerts = certStore.getMatches(signer.getSID());
//            if (matchingCerts.isEmpty()) {
//                logger.error("  ❌ Нет соответствующего сертификата!");
//            } else {
//                logger.info("  ✅ Найден соответствующий сертификат");
//            }
//        }
//
//        // 4. Проверяем содержимое
//        try {
//            CMSTypedData signedContent = cmsSignedData.getSignedContent();
//            if (signedContent != null) {
//                byte[] content = (byte[]) signedContent.getContent();
//                logger.info("Данные в CMS: {} байт", content.length);
//                logger.info("Данные: {}", new String(content, StandardCharsets.UTF_8));
//            }
//        } catch (Exception e) {
//            logger.warn("Не удалось извлечь данные: {}", e.getMessage());
//        }
//    }
//

    public boolean verifySignatureWithJCSP(String cmsBase64) throws Exception {
        final Logger logger = LoggerFactory.getLogger(this.getClass());

        try {
            byte[] cmsData = Base64.getDecoder().decode(cmsBase64);
            CMSSignedData cmsSignedData = new CMSSignedData(cmsData);

            // 1. Получаем подписанные данные
            CMSTypedData signedContent = cmsSignedData.getSignedContent();
            byte[] originalData = (byte[]) signedContent.getContent();
            logger.info("Original data length: {} bytes", originalData.length);

            // 2. Получаем сертификат
            Store certStore = cmsSignedData.getCertificates();
            SignerInformationStore signers = cmsSignedData.getSignerInfos();

            for (SignerInformation signer : signers) {
                Collection<X509CertificateHolder> certCollection = certStore.getMatches(signer.getSID());
                X509CertificateHolder certHolder = certCollection.iterator().next();

                // Конвертируем через JCSP-совместимый метод
                X509Certificate certificate = convertCertificateWithJCSP(certHolder);
                logger.info("Certificate: {}", certificate.getSubjectDN());

                // 3. Получаем подпись
                byte[] signatureBytes = signer.getSignature();
                logger.info("Signature length: {} bytes", signatureBytes.length);

                // 4. ПРОВЕРЯЕМ ЧЕРЕЗ JCSP, а не BC!
                boolean isValid = verifyWithJCSP(originalData, signatureBytes, certificate);
                logger.info("JCSP verification: {}", isValid ? "✅ УСПЕХ" : "❌ ОШИБКА");

                return isValid;
            }

            return false;

        } catch (Exception e) {
            logger.error("Verification error: {}", e.getMessage());
            return false;
        }
    }

    private X509Certificate convertCertificateWithJCSP(X509CertificateHolder certHolder) throws Exception {
        // Конвертируем без использования BC провайдера
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        return (X509Certificate) certFactory.generateCertificate(
                new ByteArrayInputStream(certHolder.getEncoded())
        );
    }

    private boolean verifyWithJCSP(byte[] data, byte[] signature, X509Certificate certificate) {
        try {
            // Определяем алгоритм based on сертификата
            String algorithm = detectSignatureAlgorithmFromCert(certificate);
            logger.info("Using algorithm for verification: {}", algorithm);

            Signature verifier = Signature.getInstance(algorithm, "JCSP");
            verifier.initVerify(certificate);
            verifier.update(data);

            return verifier.verify(signature);

        } catch (Exception e) {
            logger.error("JCSP verification failed: {}", e.getMessage());
            return false;
        }
    }

    private String detectSignatureAlgorithmFromCert(X509Certificate certificate) {
        // Анализируем OID алгоритма сертификата
        String sigAlgOID = certificate.getSigAlgOID();

        switch (sigAlgOID) {
            case "1.2.643.7.1.1.3.2": // GOST3411-2012-256withGOST3410-2012-256
                return "GOST3411_2012_256withGOST3410_2012_256";
            case "1.2.643.7.1.1.3.3": // GOST3411-2012-512withGOST3410-2012-512
                return "GOST3411_2012_512withGOST3410_2012_512";
            case "1.2.643.2.2.3": // GOST3411withGOST3410
                return "GOST3411";
            case "1.2.643.2.2.4": // GOST3411withECGOST3410
                return "GOST3411";
            default:
                return "GOST3411"; // fallback
        }
    }
}