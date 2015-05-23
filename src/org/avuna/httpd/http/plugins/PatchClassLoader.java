package org.avuna.httpd.http.plugins;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import org.avuna.httpd.util.Logger;

public class PatchClassLoader extends ClassLoader {
	public PatchClassLoader() {
		super();
	}
	
	public void loadPlugins(PatchRegistry registry, File dir) {
		for (File f : dir.listFiles()) {
			if (f.isDirectory()) {
				loadPlugins(registry, f);
			}else if (f.getName().endsWith(".class")) {
				try {
					FileInputStream fin = new FileInputStream(f);
					ByteArrayOutputStream bout = new ByteArrayOutputStream();
					int i = 1;
					byte[] buf = new byte[4096];
					while (i > 0) {
						i = fin.read(buf);
						if (i > 0) {
							bout.write(buf, 0, i);
						}
					}
					fin.close();
					byte[] j = bout.toByteArray();
					@SuppressWarnings("deprecation")
					Class<?> patchClass = defineClass(j, 0, j.length);
					javaLoaders.put(patchClass.getName(), patchClass);
					if (patchClass.isAssignableFrom(Patch.class)) { // TODO: patch priority
						registry.registerPatch((Patch)patchClass.getDeclaredConstructor(String.class).newInstance(patchClass.getName().substring(patchClass.getName().lastIndexOf(".") + 1)));
					}
				}catch (Exception e) {
					Logger.logError(e);
				}
			}
		}
	}
	
	HashMap<String, Class<?>> javaLoaders = new HashMap<String, Class<?>>();
}
