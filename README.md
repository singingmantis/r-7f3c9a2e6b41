# Ev Arşivi

MiBox / Android CloudStream için kişisel **DiziBox** ve **WebteIzle** deposu.

**Durum: ilk kaynak sürümü hazır; Android derlemesi, yayın ve MiBox oynatma
testi henüz yapılmadı. Bu klasör doğrudan kurulabilir bir eklenti değildir.**

## İlk kurulum

1. GitHub deposu `singingmantis/r-7f3c9a2e6b41` hesabında oluşturuldu:
   https://github.com/singingmantis/r-7f3c9a2e6b41
2. Bu projenin dosyalarını `main` dalına yükle. `.github/workflows/build.yml`
   ve `gradle/wrapper/gradle-wrapper.jar` dahil olmalı. `upstream-reference`,
   `.tools`, `dist`, önbellekler ve ZIP dosyaları yüklenmemeli.
   Yükleme adımını bu çalışma klasöründen asistanla birlikte yapabiliriz.
3. GitHub'da **Actions → Build and publish personal plugins** işlemini aç.
   Önce `build`, ardından `publish` işinin başarılı olmasını bekle. Kırmızı
   hata olursa ilgili işlem bağlantısını asistanla paylaş.
4. Başarılı yayın `builds` dalını kendisi oluşturur. Seçilen ad değişmezse
   CloudStream repo bağlantısı şu olacak:

   `https://raw.githubusercontent.com/singingmantis/r-7f3c9a2e6b41/builds/repo.json`

   **Bu bağlantı ilk başarılı yayından önce çalışmaz.**
5. MiBox'ta CloudStream → Ayarlar → Eklentiler → Depo ekle bölümüne bu
   bağlantıyı gir. Depodan `EvDiziBox` ve `EvWebteIzle` paketlerini yükle.
   Kaynak listesinde `DiziBox (Kişisel)` ve `WebteIzle (Kişisel)` görünür.
6. Her kaynakta arama, içerik ayrıntısı ve oynatmayı dene. DiziBox için
   sezon/bölüm listesini; WebteIzle için mevcutsa dublaj/altyazı
   seçeneklerini de kontrol et. Bir sorun olursa CloudStream sürümünü,
   içerik adını ve hata mesajını paylaş.

GitHub işlemlerinde derlemenin geçmesi sitelerin çalıştığını kanıtlamaz.
Site erişimi, oynatıcılar ve MiBox'taki CloudStream sürümü ayrıca denenir.

## Kısa kelimeyle ekleme

Kısa kelime GitHub kullanıcı veya repo adını aratmaz. İncelenen CloudStream
kodu, normal kısa kelimeleri `https://cutt.ly/KELIME` adresinden yönlendirir;
`!` ile başlayan kodlar için `https://py.md/KELIME` yolunu kullanır.

Repo yayınlandıktan sonra desteklenen hizmette uygun bir kısa bağlantı
oluşturulup gerçek `repo.json` adresine yönlendirilmeli. Hizmetin o tarihteki
özel ad/hesap koşulları ve seçilen adın müsaitliği kontrol edilecek.
Örneğin `aykutu7` seçilebilirse uygulamaya yalnızca `aykutu7` yazılır.
**`aykutu7` yalnızca örnektir; oluşturulmuş veya test edilmiş bir kod değildir.**

Kısa kodun hedefini ve CloudStream'den eklenebildiğini doğruladıktan sonra
uzun adresi tekrar yazmana gerek kalmaz. Güncellemeler aynı repo adresinden
dağıtılır. Kısa bağlantı hizmeti sorun çıkarırsa uzun bağlantı kullanılabilir.

Kaynak: https://github.com/recloudstream/cloudstream/blob/master/app/src/main/java/com/lagradost/cloudstream3/plugins/RepositoryManager.kt

## Site adresi değişince

GitHub'da `main` dalındaki **sites.properties** dosyasını açıp kalem simgesine
tıkla. İlgili satırda `=` işaretinden sonraki adresi değiştir ve kaydet:

```properties
EvDiziBox=https://www.dizibox.live
EvWebteIzle=https://webteizle.info
```

Adres `https://` ile başlamalı; sayfa yolu ve sondaki `/` bulunmamalı.
Kaydetme işlemi yeni derlemeyi başlatır; eklenti sürümü otomatik artar.
Başarılı yayından sonra CloudStream'in eklenti güncelleme kontrolünü çalıştır.
Sonuç görünmüyorsa kısa bir süre sonra tekrar dene; önbellek olabilir.

Bu dosya **mevcut iki sitenin adresini** değiştirir. Yeni bir site eklemek,
sayfa yapısındaki değişiklikleri düzeltmek veya değişen video oynatıcısını
desteklemek için kod çalışması gerekir.

## Görünürlük

Bu dağıtım yöntemi açık GitHub deposu için hazırlanmıştır. Rastgele isim,
repoyu erişime kapatmaz. Depoyu CloudStream topluluk listelerine eklemiyoruz;
yine de GitHub profilinden veya aramayla bulunabilir. Şifre, erişim anahtarı
ve kişisel oturum çerezi bu projeye eklenmemelidir.

## Teknik notlar

- İki sağlayıcı feroxx/Kekik-cloudstream kaynaklarından uyarlanmıştır;
  yazar bildirimleri korunmuştur. Ayrıntılar: [ATTRIBUTION.md](ATTRIBUTION.md).
- `sites.properties`, derleme sırasında her modülün `BuildConfig.SITE_URL`
  sabitine dönüştürülür.
- Modül kimlikleri `EvDiziBox` / `EvWebteIzle`; diğer depolardaki asıl
  modül adlarından ayrıdır.
- GitHub derlemesinde sürüm `1000 + GITHUB_RUN_NUMBER` olur. Workflow dosyasını
  silip yeniden oluşturarak sayacı sıfırlamak sürümlemeyi bozabilir.
- CloudStream Gradle eklentisi kaynak commitine sabitlenmiştir. CloudStream
  API bağımlılığı upstream'deki `pre-release` kanalını kullanır; bu bağımlılık
  değişkendir ve gelecekte uyarlama gerektirebilir.
- Yayın paketi hazırlanırken iki `.cs3` dosyasının manifest sürümleri,
  boyutları ve SHA-256 değerleri doğrulanır. Hata varsa yayın yapılmaz.
- Her yayın `source.json` içinde tam kaynak commitini kaydeder.
- Yerelde derlemek için Java 17 ve Android SDK gerekir:
  `./gradlew makePluginsJson` (Windows: `.\gradlew.bat makePluginsJson`).
- Python dağıtım kontrolleri: `python -m unittest discover -s scripts`.
  Bu kontroller sentetik test dosyaları kullanır; video oynatmaz.

## Lisans

GPL-3.0. [LICENSE](LICENSE) ve [ATTRIBUTION.md](ATTRIBUTION.md) dosyalarına bak.
