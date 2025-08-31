package ru.selsup.crypto;

import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.cms.*;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.util.CollectionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.CryptoPro.CAdES.CAdESSignature;
import ru.CryptoPro.CAdES.CAdESType;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPrivateKey;
import java.util.*;

public class JcspSigningService implements SigningService {
    public static final Logger logger = LoggerFactory.getLogger(JcspSigningService.class);
    private CertificateInfo cert;
    public String defaultAlgo;

    public JcspSigningService(StorageDiscoverer discoverer) throws NoCertificateFoundException {
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
    }

    @Override
    public CertificateInfo selectedCert() {
        return cert;
    }

    public String signData(String data, boolean detached) throws SigningException {
        try {
            // 1. Загрузка ключей и сертификатов
            KeyStore keyStore = KeyStore.getInstance(cert.getStoreType(), "JCSP");
            keyStore.load(null, null);
            // 2. Построение цепочки сертификатов
            List<X509Certificate> certChain = new ArrayList<>();
            Certificate[] certs = keyStore.getCertificateChain(cert.getAlias());
            for (Certificate cert : certs) {
                if (cert instanceof X509Certificate) {
                    certChain.add((X509Certificate) cert);
                }
            }
            List<X509CertificateHolder> bcChain = new ArrayList<>();
            for (X509Certificate cert : certChain) {
                bcChain.add(new X509CertificateHolder(cert.getEncoded()));
            }
            // 3. Получение приватного ключа
            PrivateKey privateKey = (PrivateKey) keyStore.getKey(cert.getAlias(), null);

            // 4. Создание CAdES подписи
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            CAdESSignature signature = new CAdESSignature(detached);
            signature.setCertificateStore(new CollectionStore(bcChain));

            // 5. Установка атрибутов подписи
            Hashtable<ASN1ObjectIdentifier, Attribute> table = new Hashtable<>();
            Attribute timeAttr = new Attribute(
                    CMSAttributes.signingTime,
                    new DERSet(new Time(new Date()))
            );
            table.put(timeAttr.getAttrType(), timeAttr);
            AttributeTable attrTable = new AttributeTable(table);

            // 6. Добавление подписанта
            signature.addSigner(
                    "JCSP",
                    GostOIDs.GOST3411,
                    GostOIDs.GOST3410EL,
                    privateKey,
                    certChain,
                    CAdESType.CAdES_BES,
                    null,
                    false,
                    attrTable,
                    null
            );

            // 7. Создание подписи
            signature.open(out);
            signature.update(data.getBytes(StandardCharsets.UTF_8));
            signature.close();

            byte[] signedData = out.toByteArray();
            return Base64.getEncoder().encodeToString(signedData);

        } catch (Exception e) {
            throw new SigningException("Ошибка создания CAdES подписи", e);
        }
    }

    public static class GostOIDs {
        // ГОСТ Р 34.10-2012 с ключом 256 бит
        public static final String GOST3411_2012_256_WITH_GOST3410_2012_256 = "1.2.643.7.1.1.3.2";
        // ГОСТ Р 34.10-2012 с ключом 512 бит
        public static final String GOST3411_2012_512_WITH_GOST3410_2012_512 = "1.2.643.7.1.1.3.3";
        // ГОСТ Р 34.10-2001 (старый)
        public static final String GOST3411_WITH_GOST3410 = "1.2.643.2.2.3";
        public static final String GOST3411_WITH_ECGOST3410 = "1.2.643.2.2.4";
        public static final String GOST3411 = "1.2.643.7.1.1.1.1";
        public static final String GOST3410EL = "1.2.643.7.1.1.1.2";
        // OID для атрибутов CMS
        public static final String CONTENT_TYPE = "1.2.840.113549.1.9.3";
        public static final String MESSAGE_DIGEST = "1.2.840.113549.1.9.4";
        public static final String SIGNING_TIME = "1.2.840.113549.1.9.5";
        public static final String DATA = "1.2.840.113549.1.7.1";
    }

    public class JcspAlgorithms {
        public static final String GOST3411_2012_256_WITH_GOST3410_2012_256 = "GOST3411-2012-256withGOST3410-2012-256";
        public static final String GOST3411_2012_512_WITH_GOST3410_2012_512 = "GOST3411-2012-512withGOST3410-2012-512";
        // ЕДИНСТВЕННЫЙ алгоритм хеширования в JCSP
        public static final String GOST3411 = "GOST3411"; // Автоматически выбирает размер based on контекста
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
}