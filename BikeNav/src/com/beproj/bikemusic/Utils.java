package com.beproj.bikemusic;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import android.os.Environment;

public class Utils {
	public static final Comparator<File> songFileComparator = new SongFileComparator();
	public static final Comparator<File> albumFileComparator = new AlbumFileComparator();
	

	// https://developer.android.com/guide/appendix/media-formats.html
	private static final String[] legalFormatExtensions = { "mp3", "m4p",
			"m4a", "wav", "ogg", "mkv", "3gp", "aac", "flac"};

	private static String mediaFileEndingRegex = "";
	static {
		boolean first = true;
		for (String ending : legalFormatExtensions) {
			if (!first) {
				mediaFileEndingRegex += "|" + "(\\." + ending + ")";
			} else {
				mediaFileEndingRegex += "(\\." + ending + ")";
				first = false;
			}
		}
	}

	static boolean isValidArtistDirectory(File dir) {
		if (dir == null) {
			return false;
		}

		if (!dir.isDirectory()) {
			return false;
		}

		return true;
	}

	static boolean isValidAlbumDirectory(File dir) {
		if (dir == null) {
			return false;
		}

		if (!dir.isDirectory()) {
			return false;
		}

		return true;
	}

	static boolean isValidSongFile(File song) {
		if (song == null) {
			return false;
		}

		if (!song.isFile()) {
			return false;
		}

		String name = song.getName();

		// Needs to end with one of the legal formats
		for (String ending : legalFormatExtensions) {
			if (name.endsWith("." + ending)) {
				return true;
			}
		}

		return false;
	}

	static String getPrettySongName(File songFile) {
		return getPrettySongName(songFile.getName());
	}

	static String getPrettySongName(String songName) {
		if (songName.matches("^\\d+\\s.*")) {
			return songName.replaceAll("^\\d+\\s", "").replaceAll(
					mediaFileEndingRegex, "");
		}
		return songName.replaceAll(mediaFileEndingRegex, "");
	}

	static String getArtistName(File songFile, String musicRoot) {
		File albumDir = songFile.getParentFile().getParentFile();
		if (albumDir.getAbsolutePath().equals(musicRoot)) {
			return songFile.getParentFile().getName();
		}
		return songFile.getParentFile().getParentFile().getName();
	}

	private static class SongFileComparator implements Comparator<File> {

		@Override
		public int compare(File arg0, File arg1) {
			String name0 = arg0.getName().toUpperCase(Locale.getDefault());
			String name1 = arg1.getName().toUpperCase(Locale.getDefault());
			return name0.compareTo(name1);
		}

	}

	private static class AlbumFileComparator implements Comparator<File> {

		@Override
		public int compare(File arg0, File arg1) {
			String name0 = arg0.getName().toUpperCase(Locale.getDefault());
			String name1 = arg1.getName().toUpperCase(Locale.getDefault());
			return name0.compareTo(name1);
		}

	}
	
	static File getBestGuessMusicDirectory() {
		File ext = Environment.getExternalStorageDirectory();
		if(ext != null && (ext.listFiles() != null)){
			for (File f : ext.listFiles()) {
				if (f.getName().toLowerCase(Locale.getDefault()).contains("music")) {
					return f;
				}
			}
			return new File(ext, "music");
		}
		return new File("music");
	}

	static File getRootStorageDirectory() {
		File ext = Environment.getExternalStorageDirectory();
		if(ext == null){
			ext = Environment.getRootDirectory();
			return ext;
		}
		File parent = ext.getParentFile();
		if (parent != null) {
			return parent;
		}
		return ext;
	}

	// TODO use a file filter?
	/**
	 * Returns a list of viable directories under the parent file.
	 * 
	 * @param parent
	 * @return A list of viable files. An empty list is returned if none exist.
	 */
	static List<File> getPotentialSubDirectories(File parent) {
		List<File> list = new ArrayList<File>();
		if (parent.isDirectory() && !parent.isHidden()) {
			File[] files = parent.listFiles();
			if (files != null && files.length > 0) {
				for (File f : files) {
					// If the subfolder is non-empty, add it
					if (f.isDirectory() && !f.isHidden()
							&& !f.getName().startsWith(".")) {
						if (f.listFiles() != null && f.listFiles().length > 0) {
							list.add(f);
						}
					}
				}
				return list;
			}
		}
		return list;
	}
}
