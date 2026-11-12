import re
app_id = "ca-app-pub-9771662959477951~3101815132"
banner_id = "ca-app-pub-9771662959477951/3668277990"
inter_id = "ca-app-pub-9771662959477951/6569773503"

manifest = 'app/src/main/AndroidManifest.xml'
with open(manifest, 'r') as f: c = f.read()
c = re.sub(r'<meta-data\s+android:name="com.google.android.gms.ads.APPLICATION_ID"\s+android:value="[^"]*"\s*/>', f'<meta-data android:name="com.google.android.gms.ads.APPLICATION_ID" android:value="{app_id}" />', c)
with open(manifest, 'w') as f: f.write(c)

ads = 'app/src/main/java/com/cartoniq/stealthtext/Ads.kt'
with open(ads, 'r') as f: c = f.read()
c = re.sub(r'ca-app-pub-3940256099942544/\d+', banner_id, c, count=1)
c = re.sub(r'ca-app-pub-3940256099942544/\d+', inter_id, c, count=1)
with open(ads, 'w') as f: f.write(c)
print('✅ YOUR REAL ADMOB IDS SUCCESSFULLY INJECTED!')
