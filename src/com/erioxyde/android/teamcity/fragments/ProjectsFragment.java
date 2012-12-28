package com.erioxyde.android.teamcity.fragments;

import java.util.*;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.erioxyde.android.teamcity.AboutActivity;
import com.erioxyde.android.teamcity.ProjectBuildsActivity;
import com.erioxyde.android.teamcity.R;
import com.erioxyde.android.teamcity.SettingsActivity;
import com.erioxyde.android.teamcity.bo.Build;
import com.erioxyde.android.teamcity.bo.BuildType;
import com.erioxyde.android.teamcity.bo.Project;
import com.erioxyde.android.teamcity.bo.Project.Projects;
import com.erioxyde.android.teamcity.ws.TeamCityAndroidServices;
import com.smartnsoft.droid4me.LifeCycle.BusinessObjectsRetrievalAsynchronousPolicy;
import com.smartnsoft.droid4me.app.AppPublics;
import com.smartnsoft.droid4me.cache.Values.CacheException;
import com.smartnsoft.droid4me.framework.Commands;
import com.smartnsoft.droid4me.framework.SmartAdapters;
import com.smartnsoft.droid4me.framework.SmartAdapters.BusinessViewWrapper;
import com.smartnsoft.droid4me.framework.SmartAdapters.ObjectEvent;
import com.smartnsoft.droid4me.framework.SmartAdapters.SimpleBusinessViewWrapper;
import com.smartnsoft.droid4me.menu.StaticMenuCommand;
import com.smartnsoft.droid4me.support.v4.app.SmartListViewFragment;
import com.smartnsoft.droid4me.support.v4.menu.ActionMenuCommand;

/**
 * The starting screen of the application.
 * 
 * @author Jocelyn Girard
 * @since 2012.02.23
 */
public final class ProjectsFragment extends SmartListViewFragment<Void, ListView> implements BusinessObjectsRetrievalAsynchronousPolicy {

    public static final class ProjectAttributes {

        private final TextView text1;

        private final TextView text2;

        public ProjectAttributes(View view) {
            text1 = (TextView) view.findViewById(android.R.id.text1);
            text2 = (TextView) view.findViewById(android.R.id.text2);
        }

        public void update(Project businessObject) {
            text1.setText(businessObject.name);
            text2.setText(businessObject.informations.description);
        }

    }

    private final static class ProjectWrapper extends SimpleBusinessViewWrapper<Project> {

        public ProjectWrapper(Project businessObject) {
            super(businessObject, 0, android.R.layout.simple_list_item_2);
        }

        @Override
        protected Object extractNewViewAttributes(Activity activity, View view, Project businessObject) {
            return new ProjectAttributes(view);
        }

        @Override
        protected void updateView(Activity activity, Object viewAttributes, View view, Project businessObject, int position) {
            ((ProjectAttributes) viewAttributes).update(businessObject);
        }

        @Override
        public boolean onObjectEvent(final Activity activity, Object viewAttributes, View view, final Project businessObject, ObjectEvent objectEvent, int position) {
            if (objectEvent == ObjectEvent.Clicked) {
                CharSequence[] buildTypeNames = businessObject.informations.buildTypes.getBuildTypeNames();
                if (buildTypeNames.length > 1) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                    builder.setTitle(R.string.Project_built_type).setItems(buildTypeNames, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            activity.startActivity(new Intent(activity, ProjectBuildsActivity.class).putExtra(ProjectBuildsFragment.BUILD_TYPE, businessObject.informations.buildTypes.buildType.get(which)));
                        }
                    });
                    builder.create().show();
                } else {
                    activity.startActivity(new Intent(activity, ProjectBuildsActivity.class).putExtra(ProjectBuildsFragment.BUILD_TYPE, businessObject.informations.buildTypes.buildType.get(0)));
                }
                return true;
            }
            return super.onObjectEvent(activity, viewAttributes, view, businessObject, objectEvent, position);
        }
    }

	private boolean fromCache = true;

	public List<? extends BusinessViewWrapper<?>> retrieveBusinessObjectsList()
			throws BusinessObjectUnavailableException {
      final Projects projects;
      final Set<String> hiddenProjects = getPreferences().getStringSet(SettingsActivity.HIDDEN_PROJECTS, new HashSet<String>());

      try {
          projects = TeamCityAndroidServices.getInstance().getProjects(fromCache);
          for (Project project : projects.project) {
              if (hiddenProjects.contains(project.id) == false) {
                  project.informations = TeamCityAndroidServices.getInstance().getProject(fromCache, project.id);
                  if (project.informations != null && project.informations.buildTypes != null && project.informations.buildTypes.buildType.size() > 0)
                  for (BuildType buildType : project.informations.buildTypes.buildType)
                  {
                      final Build.BuildList builds = TeamCityAndroidServices.getInstance().getBuilds(fromCache, buildType, 1);
                      if (builds.count > 0)
                      {
                         if (builds.build.get(0).status == Build.Status.SUCCESS) {
                             ++project.success;
                         }
                          else
                         {
                             ++project.errors;
                         }
                      }
                  }

              }
          }
      } catch (CacheException exception) {
          throw new BusinessObjectUnavailableException(exception);
      }

      final List<BusinessViewWrapper<?>> wrappers = new ArrayList<SmartAdapters.BusinessViewWrapper<?>>();

      for (Project project : projects.project) {
          if (hiddenProjects.contains(project.id) == false) {
              wrappers.add(new ProjectWrapper(project));
          }
      }

      fromCache  = true;
      return wrappers;
	}

    @Override
    public void onResume() {
        super.onResume();
        setHasOptionsMenu(true);
        if (isRefreshingBusinessObjectsAndDisplay() == false)
        {
            refreshBusinessObjectsAndDisplay();
        }
    }

    @Override
    public void onFulfillDisplayObjects() {
        super.onFulfillDisplayObjects();
    }

    @Override
    public void onSynchronizeDisplayObjects() {
        super.onSynchronizeDisplayObjects();
    }

    @Override
    public List<StaticMenuCommand> getMenuCommands() {
        final List<StaticMenuCommand> commands = new ArrayList<StaticMenuCommand>();
        commands.add(new ActionMenuCommand(R.string.Projects_menu_refresh, '1', 'r', android.R.drawable.ic_menu_rotate, MenuItem.SHOW_AS_ACTION_ALWAYS, new Commands.StaticEnabledExecutable() {
            @Override
            public void run() {
                fromCache = false;
                refreshBusinessObjectsAndDisplay();
            }
        }));
        return commands;
    }
}
