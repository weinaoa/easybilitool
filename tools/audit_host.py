# Copyright (c) 2026 weinaoa. Licensed under Mulan PubL v2; see LICENSE.
"""Audit declared host DEX contracts/resources without redistributing the host APK.

Usage: python tools/audit_host.py host.apk --output qa/host-9161-audit.json
Requires aapt from the project's Android SDK. This is not a device/UI test.
"""
import argparse
import hashlib
import json
import re
import struct
import subprocess
from pathlib import Path
from zipfile import ZipFile
from project import ROOT, sdk


def read_dex(data):
    def u32(at): return struct.unpack_from('<I', data, at)[0]
    def u16(at): return struct.unpack_from('<H', data, at)[0]
    def uleb(at):
        value, shift = 0, 0
        while True:
            byte = data[at]
            value |= (byte & 127) << shift
            at += 1
            if byte < 128: return value, at
            shift += 7
    strings = []
    for index in range(u32(56)):
        _, at = uleb(u32(u32(60) + index * 4))
        strings.append(data[at:data.index(0, at)].decode('utf-8', errors='replace'))
    types = [strings[u32(u32(68) + i * 4)] for i in range(u32(64))]
    protos = []
    for i in range(u32(72)):
        at = u32(76) + i * 12
        params = u32(at + 8)
        args = ''.join(types[u16(params + 4 + j * 2)] for j in range(u32(params))) if params else ''
        protos.append('(' + args + ')' + types[u32(at + 4)])
    fields, methods = [], []
    for i in range(u32(80)):
        at = u32(84) + i * 8
        fields.append(strings[u32(at + 4)] + ':' + types[u16(at + 2)])
    for i in range(u32(88)):
        at = u32(92) + i * 8
        methods.append(strings[u32(at + 4)] + protos[u16(at + 2)])
    result = {}
    for i in range(u32(96)):
        at = u32(100) + i * 32
        name, parent, cursor = types[u32(at)], u32(at + 8), u32(at + 24)
        entry = {'super': None if parent == 0xffffffff else types[parent], 'fields': {}, 'methods': {}}
        result[name] = entry
        if not cursor: continue
        sizes = []
        for _ in range(4):
            count, cursor = uleb(cursor)
            sizes.append(count)
        for kind, count in enumerate(sizes):
            index = 0
            for _ in range(count):
                delta, cursor = uleb(cursor)
                access, cursor = uleb(cursor)
                index += delta
                if kind < 2:
                    entry['fields'][fields[index]] = access
                else:
                    _, cursor = uleb(cursor)  # code_off: distinguish declarations from references.
                    entry['methods'][methods[index]] = access
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('apk', type=Path)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    classes = {}
    with ZipFile(args.apk) as archive:
        for name in archive.namelist():
            if re.fullmatch(r'classes\d*\.dex', name):
                for key, value in read_dex(archive.read(name)).items():
                    if key in classes: raise ValueError('Duplicate class: ' + key)
                    classes[key] = value
    aapt = sdk() / 'build-tools/36.0.0' / ('aapt.exe' if __import__('os').name == 'nt' else 'aapt')
    badging = subprocess.check_output([str(aapt), 'dump', 'badging', str(args.apk)], text=True)
    identity = re.search(r"package: name='([^']+)' versionCode='([^']+)' versionName='([^']+)'", badging)
    if not identity or identity[1] != 'tv.danmaku.bili': raise ValueError('Not the supported host package')
    resources = subprocess.check_output([str(aapt), 'dump', 'resources', str(args.apk)], text=True)
    checks = []
    def verify(name, result): checks.append({'target': name, 'passed': bool(result)})
    def descriptor(name): return 'L' + name.replace('.', '/') + ';'
    def member(name, kind, signature, inherited=False):
        current = descriptor(name)
        seen = set()
        while current in classes and current not in seen:
            seen.add(current)
            if signature in classes[current][kind]: return classes[current][kind][signature]
            if not inherited: break
            current = classes[current]['super']
        return None

    source = '\n'.join(p.read_text(encoding='utf-8') for p in (ROOT / 'app/src/main/kotlin').rglob('*.kt'))
    literal_classes = set(re.findall(r'"((?:androidx|com|tv)\.[\w.$\\]+)"', source))
    # Only class names, not package names, settings actions or optional pre-Theseus activity.
    excluded = {'com.android.internal.policy.PhoneWindow', 'com.bilibili.video.videodetail.VideoDetailsActivity'}
    compose = 'Lkntr/common/comment/page/ui/b0;' in classes and 'Lcom/bilibili/ship/theseus/united/page/comment/i;' in classes
    native_window = 'Lcom/bilibili/ship/theseus/united/page/activitywindow/ActivityWindowEdgeToEdgePropertyService;' in classes
    compose_home = 'Ltv/danmaku/bili/home/components/bottomtab/WindowAwareComposeView;' in classes
    for name in sorted(literal_classes):
        name = name.replace('\\$', '$')
        if not native_window and name == 'com.bilibili.ship.theseus.united.page.activitywindow.ActivityWindowEdgeToEdgePropertyService': continue
        if not native_window and name == 'androidx.core.view.DisplayCutoutCompat': continue
        if not compose_home and name.startswith(('tv.danmaku.bili.khome.widget.bottomtab.', 'tv.danmaku.bili.home.components.bottomtab.')): continue
        if not compose and name.startswith('com.bilibili.ship.theseus.united.page.comment.i'): continue
        if name in excluded or name.startswith('com.weinaoa.') or name == 'tv.danmaku.bili': continue
        verify('class ' + name, descriptor(name) in classes)

    required = {
        'androidx.recyclerview.widget.RecyclerView': ['dispatchOnScrolled(II)V'],
        'androidx.core.widget.NestedScrollView': ['onScrollChanged(IIII)V'],
        'com.google.android.material.appbar.AppBarLayout': [
            'addOnOffsetChangedListener(Lcom/google/android/material/appbar/AppBarLayout$OnOffsetChangedListener;)V',
            'removeOnOffsetChangedListener(Lcom/google/android/material/appbar/AppBarLayout$OnOffsetChangedListener;)V',
            'getTotalScrollRange()I'],
        'com.google.android.material.appbar.AppBarLayout$OnOffsetChangedListener': [
            'onOffsetChanged(Lcom/google/android/material/appbar/AppBarLayout;I)V'],
        'com.bilibili.app.preferences.BiliPreferencesActivity$BiliPreferencesFragment': ['onCreatePreferences(Landroid/os/Bundle;Ljava/lang/String;)V'],
        'com.bilibili.app.preferences.fragment.WideBiliPreferencesFragment': ['onCreatePreferences(Landroid/os/Bundle;Ljava/lang/String;)V'],
        'androidx.preference.Preference': ['<init>(Landroid/content/Context;)V', 'setKey(Ljava/lang/String;)V',
            'setTitle(Ljava/lang/CharSequence;)V', 'setSummary(Ljava/lang/CharSequence;)V', 'setOrder(I)V',
            'setOnPreferenceClickListener(Landroidx/preference/Preference$c;)V'],
        'androidx.preference.Preference$c': ['onPreferenceClick(Landroidx/preference/Preference;)Z'],
        'androidx.preference.PreferenceGroup': ['findPreference(Ljava/lang/CharSequence;)Landroidx/preference/Preference;',
            'addPreference(Landroidx/preference/Preference;)Z'],
        'com.bililive.bililive.infra.hybrid.ui.WebViewActivity': ['getLiveWeb()Lcom/bilibili/app/comm/bh/BiliWebView;'],
        'com.bilibili.app.comm.bh.BiliWebView': ['getUrl()Ljava/lang/String;',
            'evaluateJavascript(Ljava/lang/String;Lcom/bilibili/app/comm/bhwebview/api/interfaces/ValueCallback;)V'],
    }
    if compose or compose_home:
        required.update({
            'androidx.compose.ui.Modifier': ['then(Landroidx/compose/ui/Modifier;)Landroidx/compose/ui/Modifier;'],
            'androidx.compose.ui.layout.LayoutModifierKt': ['layout(Landroidx/compose/ui/Modifier;Lkotlin/jvm/functions/Function3;)Landroidx/compose/ui/Modifier;'],
            'androidx.compose.ui.layout.Measurable': ['measure-BRTryo0(J)Landroidx/compose/ui/layout/Placeable;'],
            'androidx.compose.ui.layout.Placeable': ['getWidth()I', 'getHeight()I'],
            'androidx.compose.ui.layout.MeasureScope': ['layout(IILjava/util/Map;Lkotlin/jvm/functions/Function1;)Landroidx/compose/ui/layout/MeasureResult;'],
            'androidx.compose.ui.layout.Placeable$PlacementScope': ['place(Landroidx/compose/ui/layout/Placeable;IIF)V'],
            'androidx.compose.ui.unit.Constraints': ['unbox-impl()J'],
            'androidx.compose.ui.draw.AlphaKt': ['alpha(Landroidx/compose/ui/Modifier;F)Landroidx/compose/ui/Modifier;'],
        })
        verify('Modifier.Companion', member('androidx.compose.ui.Modifier', 'fields', 'Companion:Landroidx/compose/ui/Modifier$Companion;') is not None)
    if compose:
        required.update({
            'kntr.common.comment.page.ui.c0': ['invoke(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;'],
            'kntr.common.comment.page.ui.b0': ['a(Ljava/lang/String;Lnh1/j;ZLkotlin/jvm/functions/Function1;Landroidx/compose/ui/Modifier;Lkotlin/jvm/functions/Function0;ZLandroidx/compose/foundation/layout/WindowInsets;Landroidx/compose/runtime/Composer;I)V'],
            'androidx.compose.foundation.lazy.LazyListState': ['onScroll$foundation(F)F', 'getCanScrollBackward()Z'],
            'androidx.compose.runtime.SnapshotStateKt': ['mutableStateOf$default(Ljava/lang/Object;Landroidx/compose/runtime/SnapshotMutationPolicy;ILjava/lang/Object;)Landroidx/compose/runtime/MutableState;'],
            'androidx.compose.runtime.State': ['getValue()Ljava/lang/Object;'],
            'androidx.compose.runtime.MutableState': ['setValue(Ljava/lang/Object;)V'],
            'androidx.compose.foundation.layout.SizeKt': ['height-3ABfNKs(Landroidx/compose/ui/Modifier;F)Landroidx/compose/ui/Modifier;'],
            'androidx.compose.ui.draw.ClipKt': ['clipToBounds(Landroidx/compose/ui/Modifier;)Landroidx/compose/ui/Modifier;'],
            'androidx.compose.foundation.layout.WindowInsetsKt': ['WindowInsets(IIII)Landroidx/compose/foundation/layout/WindowInsets;'],
            'com.bilibili.ship.theseus.united.page.comment.i': ['d()V'],
        })
        for signature in ['e:Lcom/bilibili/ship/theseus/united/page/comment/i$a;', 'g:I', 'h:I', 'i:I', 'j:Z', 'k:Ljava/lang/Integer;']:
            verify('Compose viewport field ' + signature, member('com.bilibili.ship.theseus.united.page.comment.i', 'fields', signature) is not None)
        verify('Compose clip field b:I', member('com.bilibili.ship.theseus.united.page.comment.i$a', 'fields', 'b:I') is not None)
        verify('Comment list owner c:LazyListState', member('kntr.common.comment.page.ui.c0', 'fields', 'c:Landroidx/compose/foundation/lazy/LazyListState;') is not None)
    if 'Lkntr/app/following/opusDetail/ui/chrome/s;' in classes:
        connection = 'kntr.app.following.opusDetail.ui.component.OpusNestPagerKt$OpusNestPager$pagerNestedScroll$1$1'
        required.update({
            'kntr.app.following.opusDetail.ui.chrome.s': ['e(LJ71/a;LJ71/m;Lkotlin/jvm/functions/Function1;Landroidx/compose/ui/Modifier;ILkotlin/jvm/functions/Function1;Landroidx/compose/runtime/Composer;I)V'],
            'androidx.compose.ui.platform.AndroidCompositionLocals_androidKt': ['getLocalView()Landroidx/compose/runtime/ProvidableCompositionLocal;'],
            'androidx.compose.runtime.Composer': ['consume(Landroidx/compose/runtime/CompositionLocal;)Ljava/lang/Object;'],
            'androidx.compose.foundation.layout.WindowInsetsPadding_androidKt': ['navigationBarsPadding(Landroidx/compose/ui/Modifier;)Landroidx/compose/ui/Modifier;'],
            'androidx.compose.foundation.layout.WindowInsetsPaddingKt': ['windowInsetsPadding(Landroidx/compose/ui/Modifier;Landroidx/compose/foundation/layout/WindowInsets;)Landroidx/compose/ui/Modifier;'],
            connection: ['onPreScroll-OzD1aCk(JI)J',
                '<init>(Lkntr/app/following/opusDetail/ui/component/e;Landroidx/compose/foundation/lazy/LazyListState;Landroidx/compose/runtime/State;Landroidx/compose/foundation/gestures/DefaultFlingBehavior;)V'],
        })
        verify('Opus list owner c:LazyListState', member(connection, 'fields', 'c:Landroidx/compose/foundation/lazy/LazyListState;') is not None)
    if 'Lkntr/common/share/api/O;' in classes:
        verify('Compose share ComponentDialog', classes['Lkntr/common/share/api/O;']['super'] == 'Landroidx/activity/ComponentDialog;')
        verify('Compose share sheet j:V', member('kntr.common.share.api.O', 'fields', 'j:Lkntr/common/share/api/V;') is not None)
        verify('Compose share sheet LinearLayout', classes.get('Lkntr/common/share/api/V;', {}).get('super') == 'Landroid/widget/LinearLayout;')
    if compose_home:
        required.update({
            'tv.danmaku.bili.khome.widget.bottomtab.h': ['invoke(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;'],
            'tv.danmaku.bili.khome.widget.bottomtab.e': ['invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;'],
            'androidx.compose.foundation.layout.PaddingKt': ['padding-qDBjuR0(Landroidx/compose/ui/Modifier;FFFF)Landroidx/compose/ui/Modifier;'],
            'androidx.compose.foundation.layout.RowKt': ['rowMeasurePolicy(Landroidx/compose/foundation/layout/Arrangement$Horizontal;Landroidx/compose/ui/Alignment$Vertical;Landroidx/compose/runtime/Composer;I)Landroidx/compose/ui/layout/MeasurePolicy;'],
            'androidx.compose.ui.ComposedModifierKt': ['materializeModifier(Landroidx/compose/runtime/Composer;Landroidx/compose/ui/Modifier;)Landroidx/compose/ui/Modifier;'],
        })
    if native_window:
        required.update({
            'com.bilibili.ship.theseus.united.page.activitywindow.ActivityWindowEdgeToEdgePropertyService': ['b(Landroidx/core/view/WindowInsetsCompat;Z)V'],
            'androidx.core.view.WindowInsetsCompat$Builder': [
                '<init>(Landroidx/core/view/WindowInsetsCompat;)V',
                'setInsets(ILandroidx/core/graphics/Insets;)Landroidx/core/view/WindowInsetsCompat$Builder;',
                'setInsetsIgnoringVisibility(ILandroidx/core/graphics/Insets;)Landroidx/core/view/WindowInsetsCompat$Builder;',
                'setDisplayCutout(Landroidx/core/view/DisplayCutoutCompat;)Landroidx/core/view/WindowInsetsCompat$Builder;',
                'build()Landroidx/core/view/WindowInsetsCompat;'],
            'androidx.core.view.WindowInsetsCompat': ['getInsets(I)Landroidx/core/graphics/Insets;',
                'getInsetsIgnoringVisibility(I)Landroidx/core/graphics/Insets;', 'isVisible(I)Z'],
            'androidx.core.view.WindowInsetsCompat$Type': ['statusBars()I', 'displayCutout()I', 'navigationBars()I', 'ime()I', 'tappableElement()I'],
            'androidx.core.graphics.Insets': ['of(IIII)Landroidx/core/graphics/Insets;'],
        })
        verify('Native window owner field d', member('com.bilibili.ship.theseus.united.page.activitywindow.ActivityWindowEdgeToEdgePropertyService', 'fields', 'd:Landroidx/activity/ComponentActivity;') is not None)
        for field in ['left:I', 'top:I', 'right:I', 'bottom:I', 'NONE:Landroidx/core/graphics/Insets;']:
            verify('Host Insets field ' + field, member('androidx.core.graphics.Insets', 'fields', field) is not None)
    for name, signatures in required.items():
        for signature in signatures:
            verify(name + '->' + signature, member(name, 'methods', signature, name in ('com.bilibili.app.comm.bh.BiliWebView', 'androidx.compose.runtime.SnapshotStateKt', 'androidx.compose.foundation.layout.WindowInsetsKt')) is not None)
    verify('AppBarLayout.currentOffset:I', member('com.google.android.material.appbar.AppBarLayout', 'fields', 'currentOffset:I') is not None)

    story = 'com.bilibili.video.story.player.StoryPlayer'
    profiles = [('a1(II)V', 'G:Landroid/content/Context;', 1), ('p3(IIIII)V', 'H:Landroid/content/Context;', 2)]
    matched = []
    for method, field, top in profiles:
        ma, fa = member(story, 'methods', method), member(story, 'fields', field)
        if ma is not None and fa is not None and not (ma & 8 or fa & 8):
            matched.append({'method': method, 'contextField': field, 'topArgument': top})
    verify('StoryPlayer verified viewport/context contract', len(matched) == 1)
    ids = set(re.findall(r'\bid\("(\w+)"\)', source)) | set(re.findall(r'getIdentifier\("(\w+)", "id"', source))
    # IDs held in arrays or compared via resource entry names.
    ids.update(['cmt3_fake_input_bar_container', 'opus_bottom_layout', 'lay_action'])
    if not compose_home: ids.discard('tab_host')
    for name in sorted(ids): verify('id/' + name, re.search(r':id/' + re.escape(name) + r'\b', resources) is not None)
    digest = hashlib.sha256()
    with args.apk.open('rb') as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b''): digest.update(block)
    report = {'package': identity[1], 'versionCode': int(identity[2]), 'versionName': identity[3],
        'apkSha256': digest.hexdigest(),
        'scope': 'Static declared DEX contracts and resource names; not runtime/UI validation',
        'storyBindings': matched, 'checks': checks, 'passed': all(c['passed'] for c in checks)}
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({'version': identity[3], 'checks': len(checks), 'failed': [c['target'] for c in checks if not c['passed']], 'story': matched}, ensure_ascii=False))
    if not report['passed']: raise SystemExit(1)


if __name__ == '__main__': main()
