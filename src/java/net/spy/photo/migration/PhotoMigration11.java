// Copyright (c) 2004  Dustin Sallings <dustin@spy.net>

package net.spy.photo.migration;

import java.sql.ResultSet;
import java.util.ArrayList;

import net.spy.db.Saver;
import net.spy.photo.PhotoConfig;
import net.spy.photo.PhotoImageFactory;
import net.spy.photo.impl.SavablePhotoImage;
import net.spy.photo.sp.migration.GetAllImgIdsAndKws;
import net.spy.util.ProgressStats;

/**
 * Migrate to the new keywords mechanism.
 */
public class PhotoMigration11 extends PhotoMigration {

	private void updateKeywords() throws Exception {
		// Get the IDs
		GetAllImgIdsAndKws db=new GetAllImgIdsAndKws(PhotoConfig.getInstance());
		ArrayList<IdKw> imgs=new ArrayList<IdKw>();
		ResultSet rs=db.executeQuery();
		while(rs.next()) {
			imgs.add(new IdKw(rs.getInt("id"), rs.getString("keywords")));
		}
		rs.close();
		db.close();

		// The saver for below
		Saver saver=new Saver(PhotoConfig.getInstance());

		ProgressStats stat=new ProgressStats(imgs.size());

		PhotoImageFactory pidf=PhotoImageFactory.getInstance();

		// Now, flip through them and set the correct value.
		for(IdKw idkw : imgs) {
			stat.start();

			SavablePhotoImage savable=new SavablePhotoImage(
				pidf.getObject(idkw.id));
			savable.setKeywords(idkw.kw);

			saver.save(savable);

			stat.stop();
			System.out.println(stat);
		}
	}

	@Override
	protected boolean checkMigration() throws Exception {
		return(getRowCount("album_keywords_map") > 0);
	}

	@Override
	protected void performMigration() throws Exception {
		updateKeywords();
	}

	/** 
	 * Run the 9th migration script.
	 */
	public static void main(String args[]) throws Exception {
		PhotoMigration11 mig=new PhotoMigration11();
		mig.migrate();
	}

	static class IdKw {
		int id=0;
		String kw=null;

		public IdKw(int i, String k) {
			super();
			this.id=i;
			this.kw=k;
		}
	}

}
